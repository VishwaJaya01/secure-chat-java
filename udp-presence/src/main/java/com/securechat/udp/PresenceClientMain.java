package com.securechat.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone launcher for the UDP presence listener so operators can watch beacons separately.
 */
public final class PresenceClientMain {
    private static final Logger log = LoggerFactory.getLogger(PresenceClientMain.class);

    private PresenceClientMain() {
    }

    public static void main(String[] args) {
        int port = 9090;
        long offlineThresholdMs = 15000;
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--offline-ms=")) {
                offlineThresholdMs = Long.parseLong(arg.substring("--offline-ms=".length()));
            }
        }

        OnlineTracker tracker = new OnlineTracker(offlineThresholdMs);
        PresenceClient client = new PresenceClient(port, serverId -> {
            tracker.markSeen(serverId);
            tracker.logSummary();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
            log.info("Presence listener stopped.");
        }, "udp-presence-client-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("UDP Presence Client listening on port {}", port);
        log.info("Showing peers seen in the last {} ms", offlineThresholdMs);
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");
        client.run();
    }

    private static class OnlineTracker {
        private final java.util.Map<String, Long> lastSeen = new java.util.concurrent.ConcurrentHashMap<>();
        private final long ttlMs;

        OnlineTracker(long ttlMs) {
            this.ttlMs = ttlMs;
        }

        void markSeen(String serverId) {
            lastSeen.put(serverId, System.currentTimeMillis());
        }

        void logSummary() {
            long cutoff = System.currentTimeMillis() - ttlMs;
            lastSeen.entrySet().removeIf(entry -> entry.getValue() < cutoff);
            var online = lastSeen.keySet().stream()
                    .sorted()
                    .toList();
            if (online.isEmpty()) {
                log.info("📡 No peers online (waiting for beacons)");
            } else {
                log.info("📡 ONLINE ({}) :: {}", online.size(), String.join(", ", online));
            }
        }
    }
}
