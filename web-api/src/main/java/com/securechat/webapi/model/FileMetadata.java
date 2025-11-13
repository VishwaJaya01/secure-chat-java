package com.securechat.webapi.model;

import java.time.Instant;

public record FileMetadata(
    String fileId,
    String filename,
    long fileSize,
    String owner,
    String tcpHost,
    int tcpPort,
    Instant createdAt
) {
    public static FileMetadata create(String filename, long fileSize, String owner, String tcpHost, int tcpPort) {
        String fileId = java.util.UUID.randomUUID().toString();
        return new FileMetadata(fileId, filename, fileSize, owner, tcpHost, tcpPort, Instant.now());
    }
}
