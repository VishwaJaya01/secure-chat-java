package com.securechat.announce.nio.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securechat.core.Announcement;
import com.securechat.core.AnnouncementBroadcastHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple SSE client that subscribes to the Spring Boot announcement stream and
 * re-broadcasts events to the local AnnouncementBroadcastHub so the standalone
 * NIO gateway mirrors whatever the API emits.
 */
public class AnnouncementSseBridge implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementSseBridge.class);

    private final String streamUrl;
    private final AnnouncementBroadcastHub broadcastHub;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ObjectMapper mapper = new ObjectMapper();
    private Thread worker;

    public AnnouncementSseBridge(String streamUrl, AnnouncementBroadcastHub broadcastHub) {
        this.streamUrl = streamUrl;
        this.broadcastHub = broadcastHub;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(this::runLoop, "AnnouncementSseBridge");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private void runLoop() {
        while (running.get()) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(streamUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(0);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    log.info("SSE bridge connected to {}", streamUrl);
                    String line;
                    StringBuilder dataBuffer = new StringBuilder();
                    while (running.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith(":")) {
                            // comment / heartbeat
                            continue;
                        }
                        if (line.startsWith("event:")) {
                            // ignore event label
                            continue;
                        }
                        if (line.startsWith("data:")) {
                            dataBuffer.append(line.substring(5).trim());
                            continue;
                        }
                        if (line.isEmpty()) {
                            // end of event
                            if (dataBuffer.length() > 0) {
                                handlePayload(dataBuffer.toString());
                                dataBuffer.setLength(0);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("SSE bridge disconnected from {} ({}) – retrying in 2s", streamUrl, e.getMessage());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (running.get()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void handlePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            JsonNode node = mapper.readTree(payload);
            Announcement announcement = new Announcement(
                    node.path("id").isMissingNode() ? null : node.path("id").asLong(),
                    node.path("author").asText("Unknown"),
                    node.path("title").asText("Untitled"),
                    node.path("content").asText(""),
                    node.hasNonNull("createdAt")
                            ? Instant.parse(node.get("createdAt").asText())
                            : Instant.now()
            );
            broadcastHub.broadcast(announcement);
            log.info("SSE bridge relayed announcement #{} '{}' to local NIO gateway",
                    announcement.getId(), announcement.getTitle());
        } catch (Exception e) {
            log.warn("Failed to parse SSE payload: {}", payload, e);
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
