package com.securechat.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Announcement model for admin broadcasts.
 */
public class Announcement {
    private final Long id;
    private final String author;
    private final String title;
    private final String content;
    private final Instant createdAt;

    public Announcement(Long id, String author, String title, String content, Instant createdAt) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Announcement(String author, String title, String content) {
        this(null, author, title, content, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Announcement that = (Announcement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Announcement{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}




