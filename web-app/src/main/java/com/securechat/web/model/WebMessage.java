package com.securechat.web.model;

import java.time.Instant;
import java.util.Objects;

public record WebMessage(String user, String text, Instant timestamp, boolean mine, String type) {

    public WebMessage {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(type, "type");
    }

    public static WebMessage chat(String user, String text) {
        return new WebMessage(user, text, Instant.now(), false, "msg");
    }

    public static WebMessage system(String text) {
        return new WebMessage("system", text, Instant.now(), false, "system");
    }

    public WebMessage withTimestamp(Instant ts) {
        return new WebMessage(user, text, ts, mine, type);
    }

    public WebMessage withMine(boolean mineFlag) {
        return new WebMessage(user, text, timestamp, mineFlag, type);
    }
}
