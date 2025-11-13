package com.securechat.task;

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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CLI monitor that tails the /api/tasks/stream SSE endpoint so task lifecycle
 * events are visible in a dedicated terminal, even if only the Spring Boot
 * instance owns the business logic.
 */
public final class TaskSseMonitor {
    private static final Logger log = LoggerFactory.getLogger(TaskSseMonitor.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AtomicBoolean running = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {
        String sseUrl = "http://localhost:8080/api/tasks/stream";
        int reconnectDelayMs = 2000;

        for (String arg : args) {
            if (arg.startsWith("--sse-url=")) {
                sseUrl = arg.substring("--sse-url=".length());
            } else if (arg.startsWith("--reconnect-ms=")) {
                reconnectDelayMs = Integer.parseInt(arg.substring("--reconnect-ms=".length()));
            }
        }

        TaskSseMonitor monitor = new TaskSseMonitor();
        monitor.start(sseUrl, reconnectDelayMs);
    }

    private void start(String sseUrl, int reconnectDelayMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            latch.countDown();
        }, "task-sse-monitor-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("Task Board SSE monitor listening to {}", sseUrl);
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");

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
                    log.info("📡 Connected to task stream {}", sseUrl);
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
                    log.warn("Task SSE connection dropped ({}). Reconnecting in {}ms...", e.getMessage(), reconnectDelayMs);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (running.get()) {
                try {
                    Thread.sleep(Math.max(500, reconnectDelayMs));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        latch.await();
    }

    private void handleEvent(String event, String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        if (!"task".equals(event)) {
            log.debug("Ignoring SSE event {} with payload {}", event, payload);
            return;
        }
        try {
            TaskPayload task = mapper.readValue(payload, TaskPayload.class);
            log.info("🧩 TASK EVENT #{} '{}' [{}] assignee={} createdBy={} updated={}",
                    task.id(),
                    task.title(),
                    task.status(),
                    task.assignee() != null ? task.assignee() : "(unassigned)",
                    task.createdBy(),
                    formatInstant(task.updatedAt()));
            if (task.description() != null && !task.description().isBlank()) {
                log.debug("   ↳ {}", task.description());
            }
        } catch (Exception e) {
            log.warn("Failed to parse task payload {}", payload, e);
        }
    }

    private String formatInstant(Instant instant) {
        try {
            return instant != null ? FORMATTER.format(instant) : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private record TaskPayload(
            Long id,
            String title,
            String description,
            String status,
            String assignee,
            String createdBy,
            Instant createdAt,
            Instant updatedAt
    ) { }
}
