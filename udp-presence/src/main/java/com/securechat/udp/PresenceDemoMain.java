package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience launcher that can run the UDP Presence server, client, or both
 * in a single console so demos don't need multiple terminals.
 */
public final class PresenceDemoMain {
    private static final Logger log = LoggerFactory.getLogger(PresenceDemoMain.class);

    private PresenceDemoMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 9090;
        String serverId = "server-8080";
        long intervalMs = 3000;
        long offlineMs = 15000;
        Mode mode = Mode.BOTH;

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--server-id=")) {
                serverId = arg.substring("--server-id=".length());
            } else if (arg.startsWith("--interval-ms=")) {
                intervalMs = Long.parseLong(arg.substring("--interval-ms=".length()));
            } else if (arg.startsWith("--offline-ms=")) {
                offlineMs = Long.parseLong(arg.substring("--offline-ms=".length()));
            } else if (arg.startsWith("--mode=")) {
                mode = Mode.valueOf(arg.substring("--mode=".length()).toUpperCase());
            }
        }

        log.info("───────────────────────────────────────────────────────────────");
        log.info("UDP Presence Demo (mode={}) on port {}", mode, port);
        log.info("Server id: {}, Broadcast every {} ms, Offline window {} ms", serverId, intervalMs, offlineMs);
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");

        int actualPort = port;
        if (mode.includesClient() && !isPortAvailable(port)) {
            actualPort = port + 1;
            log.info("Port {} already in use; running demo on {}", port, actualPort);
        }

        PresenceServer server = mode.includesServer() ? new PresenceServer(actualPort, serverId, intervalMs) : null;
        PresenceClient client = null;
        if (mode.includesClient()) {
            OnlineTracker tracker = new OnlineTracker(offlineMs);
            client = new PresenceClient(actualPort, tracker::handleBeacon);
            log.info("UDP Presence Client listening on port {}", actualPort);
        }

        Thread serverThread = null;
        Thread clientThread = null;

        if (server != null) {
            serverThread = new Thread(server, "udp-presence-server");
            serverThread.start();
        }
        if (client != null) {
            clientThread = new Thread(client, "udp-presence-client");
            clientThread.start();
        }

        PresenceServer finalServer = server;
        PresenceClient finalClient = client;
        Thread finalServerThread = serverThread;
        Thread finalClientThread = clientThread;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (finalServer != null) {
                    finalServer.close();
                }
                if (finalClient != null) {
                    finalClient.close();
                }
                if (finalServerThread != null) {
                    finalServerThread.join(500);
                }
                if (finalClientThread != null) {
                    finalClientThread.join(500);
                }
            } catch (Exception e) {
                log.warn("Error shutting down presence demo", e);
            }
            log.info("Presence demo stopped");
        }, "udp-presence-demo-shutdown"));

        if (serverThread != null) {
            serverThread.join();
        }
        if (clientThread != null) {
            clientThread.join();
        }
    }

    private static boolean isPortAvailable(int port) {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new java.net.InetSocketAddress(port));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private enum Mode {
        SERVER, CLIENT, BOTH;

        boolean includesServer() {
            return this == SERVER || this == BOTH;
        }

        boolean includesClient() {
            return this == CLIENT || this == BOTH;
        }
    }

    private static class OnlineTracker {
        private final java.util.Map<String, Long> lastSeen = new java.util.concurrent.ConcurrentHashMap<>();
        private final long ttlMs;

        OnlineTracker(long ttlMs) {
            this.ttlMs = ttlMs;
        }

        void handleBeacon(String serverId) {
            lastSeen.put(serverId, System.currentTimeMillis());
            logSummary();
        }

        void logSummary() {
            long cutoff = System.currentTimeMillis() - ttlMs;
            lastSeen.entrySet().removeIf(entry -> entry.getValue() < cutoff);
            var online = lastSeen.keySet().stream().sorted().toList();
            if (online.isEmpty()) {
                log.info("📡 ONLINE (0) :: waiting for beacons");
            } else {
                log.info("📡 ONLINE ({}) :: {}", online.size(), String.join(", ", online));
            }
        }
    }
}
