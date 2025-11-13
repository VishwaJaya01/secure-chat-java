package com.securechat.webapi.telemetry;

import java.time.Instant;
import java.util.List;

public record NetworkServiceSnapshot(
        String key,
        String name,
        String protocol,
        String endpoint,
        String description,
        ServiceState state,
        Instant startedAt,
        Instant lastUpdated,
        long usageCount,
        String lastUsage,
        List<String> recentEvents
) {}
