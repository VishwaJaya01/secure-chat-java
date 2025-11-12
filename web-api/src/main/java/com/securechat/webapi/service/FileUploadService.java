package com.securechat.webapi.service;

import com.securechat.tcp.FileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.ConnectException;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private final FileClient fileClient;
    private final String tcpHost;
    private final int tcpPort;

    public FileUploadService(
        @Value("${file.transfer.tcp.host:localhost}") String tcpHost,
        @Value("${file.transfer.tcp.port:6000}") int tcpPort
    ) {
        this.tcpHost = tcpHost;
        this.tcpPort = tcpPort;
        this.fileClient = new FileClient(tcpHost, tcpPort);
        log.info("FileUploadService initialized: connecting to {}:{}", tcpHost, tcpPort);
    }

    public void upload(MultipartFile file, String owner) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
        String normalizedOwner = owner == null ? "" : owner.trim();
        if (normalizedOwner.isEmpty()) {
            throw new IllegalArgumentException("Owner must not be blank");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = file.getName();
        }
        
        try {
            log.info("Uploading file: {} ({} bytes) from owner: {}", filename, file.getSize(), normalizedOwner);
            fileClient.sendStream(filename, file.getSize(), file.getInputStream(), normalizedOwner);
            log.info("File upload completed successfully: {}", filename);
        } catch (ConnectException e) {
            log.error("Failed to connect to TCP file server at {}:{}. Is FileServer running?", tcpHost, tcpPort, e);
            throw new IOException("File server is not available. Please ensure the TCP file server is running on port " + tcpPort, e);
        } catch (IOException e) {
            log.error("Error uploading file {}: {}", filename, e.getMessage(), e);
            throw new IOException("Failed to upload file: " + e.getMessage(), e);
        }
    }
}

