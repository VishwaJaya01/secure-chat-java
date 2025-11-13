package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * UDP presence client that listens for presence beacons and notifies a handler.
 */
public class PresenceClient implements Runnable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PresenceClient.class);
    private final int port;
    private final Consumer<String> presenceHandler;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public PresenceClient(int port, Consumer<String> presenceHandler) {
        this.port = port;
        this.presenceHandler = presenceHandler;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(null);
            socket.bind(new InetSocketAddress(port));
            socket.setBroadcast(true);
            log.info("UDP Presence Client listening on port {}", port);
            
            byte[] buffer = new byte[256];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.startsWith("PRESENCE:")) {
                        String serverId = message.substring("PRESENCE:".length());
                        // Only log at debug level - actual login will be logged by PresenceService when status changes
                        log.debug("📡 UDP PRESENCE BEACON: Received from {} → Server ID: {}", 
                            packet.getAddress().getHostAddress(), serverId);
                        presenceHandler.accept(serverId);
                    }
                } catch (IOException e) {
                    if (running) {
                        log.warn("Error receiving presence beacon", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not start UDP Presence Client on port {}", port, e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        log.info("UDP Presence Client stopped");
    }
}
