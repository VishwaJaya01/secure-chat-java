package com.securechat.webapi.telemetry;

import java.time.Instant;

public record LinkPreviewEvent(
        Instant timestamp,
        String url,
        String title,
        String status,
        String details
) {
    public static LinkPreviewEvent of(String url, String title, String status, String details) {
        return new LinkPreviewEvent(Instant.now(), url, title, status, details);
    }
}
