package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP presence server that periodically broadcasts availability beacons.
 */
public class PresenceServer implements Runnable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PresenceServer.class);
    private final int port;
    private final String serverId;
    private final long intervalMillis;
    private volatile boolean running = true;

    public PresenceServer(int port, String serverId) {
        this(port, serverId, 5000); // Default: broadcast every 5 seconds
    }

    public PresenceServer(int port, String serverId, long intervalMillis) {
        this.port = port;
        this.serverId = serverId;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            log.info("UDP Presence Server started on port {}, broadcasting every {}ms", port, intervalMillis);
            
            while (running) {
                try {
                    String payload = "PRESENCE:" + serverId;
                    byte[] payloadBytes = payload.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                        payloadBytes, 
                        payloadBytes.length, 
                        InetAddress.getByName("255.255.255.255"), 
                        port
                    );
                    socket.send(packet);
                    log.debug("Sent presence beacon: {}", payload);
                    
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    if (running) {
                        log.warn("Presence broadcast failed", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not start UDP Presence Server on port {}", port, e);
        }
    }

    @Override
    public void close() {
        running = false;
        log.info("UDP Presence Server stopped");
    }
}
