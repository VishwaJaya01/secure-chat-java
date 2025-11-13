package com.securechat.linkpreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LinkPreviewMonitor {
    private static final Logger log = LoggerFactory.getLogger(LinkPreviewMonitor.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AtomicBoolean running = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {
        String sseUrl = "http://localhost:8080/api/link/preview/stream";
        for (String arg : args) {
            if (arg.startsWith("--sse-url=")) {
                sseUrl = arg.substring("--sse-url=".length());
            }
        }
        new LinkPreviewMonitor().start(sseUrl);
    }

    private void start(String sseUrl) throws InterruptedException {
        log.info("───────────────────────────────────────────────────────────────");
        log.info("Link Preview monitor listening to {}", sseUrl);
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            latch.countDown();
        }, "link-preview-monitor-shutdown"));

        running.set(true);
        while (running.get()) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sseUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(0);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    log.info("📡 Connected to {}", sseUrl);
                    String line;
                    String event = "message";
                    StringBuilder data = new StringBuilder();
                    while (running.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith(":")) {
                            continue;
                        }
                        if (line.startsWith("event:")) {
                            event = line.substring(6).trim();
                            continue;
                        }
                        if (line.startsWith("data:")) {
                            if (data.length() > 0) {
                                data.append('\n');
                            }
                            data.append(line.substring(5).trim());
                            continue;
                        }
                        if (line.isEmpty()) {
                            if (data.length() > 0) {
                                handleEvent(event, data.toString());
                                data.setLength(0);
                            }
                            event = "message";
                        }
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("Link preview SSE disconnected ({}). Retrying in 2s...", e.getMessage());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (running.get()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        latch.await();
    }

    private void handleEvent(String event, String payload) {
        if (!"link-preview".equals(event) || payload == null || payload.isBlank()) {
            return;
        }
        try {
            PreviewEvent previewEvent = mapper.readValue(payload, PreviewEvent.class);
            log.info("🔗 {} → '{}' [{}] {}",
                    previewEvent.url(),
                    previewEvent.title() != null ? previewEvent.title() : "(untitled)",
                    previewEvent.status(),
                    previewEvent.details());
            log.debug("   @ {}", FORMATTER.format(previewEvent.timestamp()));
        } catch (Exception e) {
            log.warn("Failed to parse link preview event {}", payload, e);
        }
    }

    private record PreviewEvent(
            java.time.Instant timestamp,
            String url,
            String title,
            String status,
            String details
    ) { }
}
