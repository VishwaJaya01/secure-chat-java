package com.securechat.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Placeholder immutable message model shared across chat modules.
 */
public class Message {
    private final String from;
    private final String content;
    private final Instant timestamp;

    public Message(String from, String content, Instant timestamp) {
        this.from = from;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getFrom() {
        return from;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return Objects.equals(from, message.from)
                && Objects.equals(content, message.content)
                && Objects.equals(timestamp, message.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, content, timestamp);
    }

    @Override
    public String toString() {
        return "Message{"
                + "from='" + from + '\''
                + ", content='" + content + '\''
                + ", timestamp=" + timestamp
                + '}';
    }
}
