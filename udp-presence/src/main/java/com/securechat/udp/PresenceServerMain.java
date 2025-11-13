package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone launcher for the UDP presence beacon broadcaster.
 */
public final class PresenceServerMain {
    private static final Logger log = LoggerFactory.getLogger(PresenceServerMain.class);

    private PresenceServerMain() {
    }

    public static void main(String[] args) {
        int port = 9090;
        long intervalMs = 3000;
        String serverId = "server-8080";

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--server-id=")) {
                serverId = arg.substring("--server-id=".length());
            } else if (arg.startsWith("--interval-ms=")) {
                intervalMs = Long.parseLong(arg.substring("--interval-ms=".length()));
            }
        }

        PresenceServer server = new PresenceServer(port, serverId, intervalMs);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
            log.info("Presence beacon stopped.");
        }, "udp-presence-server-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("UDP Presence Server broadcasting '{}' every {}ms on port {}", serverId, intervalMs, port);
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");
        server.run();
    }
}
