package com.securechat.announce.nio;

import com.securechat.announce.nio.bridge.AnnouncementSseBridge;
import com.securechat.core.Announcement;
import com.securechat.core.AnnouncementBroadcastHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Standalone launcher for the NIO announcement gateway so it can run from its own console.
 * Optionally tails the Spring Boot SSE stream (default http://localhost:8080/api/announcements/stream)
 * so every announcement the API emits is mirrored to any connected NIO clients and logged here.
 */
public final class AnnouncementGatewayMain {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementGatewayMain.class);

    private AnnouncementGatewayMain() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 8081;
        String sseUrl = "http://localhost:8080/api/announcements/stream";

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--sse-url=")) {
                sseUrl = arg.substring("--sse-url=".length());
            } else if (arg.equalsIgnoreCase("--no-sse")) {
                sseUrl = null;
            }
        }

        AnnouncementBroadcastHub hub = new AnnouncementBroadcastHub();
        AnnouncementGateway gateway = new AnnouncementGateway(
                port,
                hub,
                raw -> log.info("📢 Announcement payload received from client: {}", raw)
        );
        hub.register(AnnouncementGatewayMain::logBroadcast);

        AnnouncementSseBridge sseBridge = null;
        if (sseUrl != null && !sseUrl.isBlank()) {
            sseBridge = new AnnouncementSseBridge(sseUrl, hub);
            sseBridge.start();
            log.info("Bridging announcements from SSE stream: {}", sseUrl);
        } else {
            log.info("SSE bridge disabled (use --sse-url=<url> to enable)");
        }

        CountDownLatch latch = new CountDownLatch(1);
        AnnouncementSseBridge finalSseBridge = sseBridge;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                gateway.close();
            } catch (IOException e) {
                log.warn("Failed to stop announcement gateway cleanly", e);
            }
            if (finalSseBridge != null) {
                finalSseBridge.close();
            }
            latch.countDown();
        }, "announcement-gateway-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("AnnouncementGateway listening on port {}", port);
        log.info("Connect TCP clients to push announcements or watch logs here.");
        if (sseUrl != null) {
            log.info("Mirroring announcements from {}", sseUrl);
        }
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");

        gateway.start();
        latch.await();
    }

    private static void logBroadcast(Announcement announcement) {
        log.info("📣 Broadcast → {}: {}", announcement.getTitle(), announcement.getContent());
    }
}
