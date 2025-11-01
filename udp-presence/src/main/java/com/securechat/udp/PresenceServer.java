package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Placeholder UDP presence server that broadcasts availability beacons.
 */
public class PresenceServer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PresenceServer.class);
    private final int port;
    private final byte[] payload = "SECURE_CHAT_HELLO".getBytes();

    public PresenceServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(payload, payload.length, InetAddress.getByName("255.255.255.255"), port);
            socket.setBroadcast(true);
            socket.send(packet);
            log.info("Sent placeholder presence beacon on port {}", port);
        } catch (IOException e) {
            log.warn("Presence broadcast failed", e);
        }
    }
}
