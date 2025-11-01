package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * Placeholder UDP presence listener.
 */
public class PresenceClient implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PresenceClient.class);
    private final int port;

    public PresenceClient(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.bind(new InetSocketAddress(port));
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            log.info("Received presence beacon from {}", packet.getAddress());
        } catch (IOException e) {
            log.warn("Presence client failed", e);
        }
    }
}
