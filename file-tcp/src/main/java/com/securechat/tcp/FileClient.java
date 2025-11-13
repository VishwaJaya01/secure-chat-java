package com.securechat.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Objects;

public class FileClient {

    private static final Logger log = LoggerFactory.getLogger(FileClient.class);
    private static final int CHUNK_SIZE = 8192;

    private final String host;
    private final int port;

    public FileClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendFile(File file, String owner) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            sendStream(file.getName(), file.length(), fis, owner);
        }
    }

    public void sendStream(String filename, long fileSize, InputStream inputStream, String owner) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must not be negative");
        }
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(owner, "owner must not be null");

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             InputStream in = inputStream) {

            out.writeUTF(filename);
            out.writeLong(fileSize);
            out.writeUTF(owner);

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            long totalBytesSent = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                ChunkFramer.writeChunk(out, chunk);
                totalBytesSent += bytesRead;
            }
            if (fileSize != 0 && totalBytesSent != fileSize) {
                log.warn("Expected to send {} bytes but transferred {}", fileSize, totalBytesSent);
            }
            log.info("File {} sent successfully", filename);
        }
    }
}
