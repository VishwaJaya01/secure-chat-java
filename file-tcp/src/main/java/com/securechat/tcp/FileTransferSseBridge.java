package com.securechat.tcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.securechat.core.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight SSE client that tails the web-api /api/files/stream endpoint so
 * we can mirror file announcements/downloads in the standalone console even
 * when the embedded FileServer owns the TCP port.
 */
public final class FileTransferSseBridge implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FileTransferSseBridge.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final String streamUrl;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private Thread worker;

    public FileTransferSseBridge(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(this::runLoop, "FileTransferSseBridge");
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
                    log.info("📡 File SSE bridge connected to {}", streamUrl);
                    String line;
                    String currentEvent = "message";
                    StringBuilder data = new StringBuilder();
                    while (running.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith(":")) {
                            continue; // comment/heartbeat
                        }
                        if (line.startsWith("event:")) {
                            currentEvent = line.substring(6).trim();
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
                                handleEvent(currentEvent, data.toString());
                                data.setLength(0);
                            }
                            currentEvent = "message";
                        }
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("File SSE bridge disconnected ({}) – retrying in 2s", e.getMessage());
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

    private void handleEvent(String event, String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            switch (event) {
                case "files-init" -> {
                    List<FileMetadata> files = mapper.readValue(payload, new TypeReference<List<FileMetadata>>() {});
                    log.info("📁 Initial file inventory ({} entries)", files.size());
                    files.stream().limit(3).forEach(f ->
                            log.info("   • {} ({} bytes) by {} @ {}:{} [{}]",
                                    f.filename(), f.fileSize(), f.owner(), f.tcpHost(), f.tcpPort(),
                                    formatInstant(f.createdAt())));
                    if (files.size() > 3) {
                        log.info("   • …and {} more", files.size() - 3);
                    }
                }
                case "file-announced" -> {
                    FileMetadata meta = mapper.readValue(payload, FileMetadata.class);
                    log.info("📤 FILE AVAILABLE: {} ({} bytes) uploaded by {} @ {}:{} [{}]",
                            meta.filename(), meta.fileSize(), meta.owner(), meta.tcpHost(), meta.tcpPort(),
                            formatInstant(meta.createdAt()));
                }
                case "file-removed" -> {
                    FileMetadata meta = mapper.readValue(payload, FileMetadata.class);
                    log.info("🗑️ FILE REMOVED: {} ({}) at {}", meta.filename(), meta.fileId(), meta.createdAt());
                }
                default -> log.debug("Ignoring SSE event {} with payload {}", event, payload);
            }
        } catch (Exception e) {
            log.warn("Failed to parse SSE event {} payload {}", event, payload, e);
        }
    }

    private String formatInstant(Instant instant) {
        try {
            return instant != null ? FORMATTER.format(instant) : "-";
        } catch (Exception e) {
            return "-";
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
