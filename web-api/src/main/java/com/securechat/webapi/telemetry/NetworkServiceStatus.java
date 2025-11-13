package com.securechat.webapi.telemetry;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

class NetworkServiceStatus {
    private static final int MAX_EVENTS = 8;

    private final String key;
    private final String name;
    private final String protocol;
    private final String endpoint;
    private final String description;
    private final Deque<String> recentEvents = new ArrayDeque<>();
    private final AtomicLong usageCount = new AtomicLong(0);

    private volatile ServiceState state = ServiceState.STARTING;
    private volatile Instant startedAt;
    private volatile Instant lastUpdated = Instant.now();
    private volatile String lastUsage = "-";

    NetworkServiceStatus(String key, String name, String protocol, String endpoint, String description) {
        this.key = key;
        this.name = name;
        this.protocol = protocol;
        this.endpoint = endpoint;
        this.description = description;
    }

    String key() {
        return key;
    }

    String name() {
        return name;
    }

    String protocol() {
        return protocol;
    }

    String endpoint() {
        return endpoint;
    }

    String description() {
        return description;
    }

    ServiceState state() {
        return state;
    }

    synchronized void markState(ServiceState newState, String event) {
        if (newState == ServiceState.RUNNING && startedAt == null) {
            startedAt = Instant.now();
        }
        state = newState;
        appendEvent(event);
    }

    synchronized void markDisabled(String reason) {
        state = ServiceState.DISABLED;
        appendEvent(reason);
    }

    synchronized void recordUsage(String usage) {
        long count = usageCount.incrementAndGet();
        lastUsage = usage;
        appendEvent("Usage #" + count + ": " + usage);
    }

    synchronized void appendEvent(String event) {
        String timestamped = DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + " — " + event;
        if (recentEvents.size() == MAX_EVENTS) {
            recentEvents.removeLast();
        }
        recentEvents.addFirst(timestamped);
        lastUpdated = Instant.now();
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant lastUpdated() {
        return lastUpdated;
    }

    long usageCount() {
        return usageCount.get();
    }

    String lastUsage() {
        return lastUsage;
    }

    synchronized List<String> recentEventsSnapshot() {
        return new ArrayList<>(recentEvents);
    }

    NetworkServiceSnapshot snapshot() {
        return new NetworkServiceSnapshot(
                key,
                name,
                protocol,
                endpoint,
                description,
                state,
                startedAt,
                lastUpdated,
                usageCount(),
                lastUsage,
                recentEventsSnapshot()
        );
    }
}
