package com.securechat.webapi.service;

import com.securechat.tcp.FileClient;
import com.securechat.webapi.telemetry.NetworkServiceKeys;
import com.securechat.webapi.telemetry.NetworkServiceRegistry;
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
    private final boolean fileTransferEnabled;
    private final NetworkServiceRegistry networkServiceRegistry;

    public FileUploadService(
        @Value("${file.transfer.tcp.host:localhost}") String tcpHost,
        @Value("${file.transfer.tcp.port:6000}") int tcpPort,
        @Value("${file.transfer.enabled:true}") boolean fileTransferEnabled,
        NetworkServiceRegistry networkServiceRegistry
    ) {
        this.tcpHost = tcpHost;
        this.tcpPort = tcpPort;
        this.fileTransferEnabled = fileTransferEnabled;
        this.networkServiceRegistry = networkServiceRegistry;
        this.fileClient = new FileClient(tcpHost, tcpPort);
        log.info("FileUploadService initialized: connecting to {}:{}", tcpHost, tcpPort);
        networkServiceRegistry.registerService(
            NetworkServiceKeys.TCP_FILE_CLIENT,
            "TCP File Transfer Client",
            "TCP",
            tcpHost + ":" + tcpPort,
            "Streams files to the FileServer for collaboration"
        );
        if (fileTransferEnabled) {
            networkServiceRegistry.serviceRunning(
                NetworkServiceKeys.TCP_FILE_CLIENT,
                "Ready to stream attachments to tcp://" + tcpHost + ":" + tcpPort
            );
        } else {
            networkServiceRegistry.serviceDisabled(
                NetworkServiceKeys.TCP_FILE_CLIENT,
                "file.transfer.enabled=false"
            );
        }
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
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.TCP_FILE_SERVER,
                "UPLOAD-REQUEST",
                String.format("%s streaming %s (%d bytes) via tcp://%s:%d", normalizedOwner, filename, file.getSize(), tcpHost, tcpPort)
            );
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.TCP_FILE_CLIENT,
                "STREAM",
                String.format("Uploading %s (%d bytes) to tcp://%s:%d", filename, file.getSize(), tcpHost, tcpPort)
            );
            fileClient.sendStream(filename, file.getSize(), file.getInputStream(), normalizedOwner);
            log.info("File upload completed successfully: {}", filename);
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.TCP_FILE_CLIENT,
                "COMPLETE",
                "Upload finished for " + filename
            );
        } catch (ConnectException e) {
            log.error("Failed to connect to TCP file server at {}:{}. Is FileServer running?", tcpHost, tcpPort, e);
            networkServiceRegistry.serviceDegraded(
                NetworkServiceKeys.TCP_FILE_SERVER,
                "Upload failed - connection refused on port " + tcpPort
            );
            networkServiceRegistry.serviceDegraded(
                NetworkServiceKeys.TCP_FILE_CLIENT,
                "Could not reach tcp://" + tcpHost + ":" + tcpPort + " (" + e.getMessage() + ")"
            );
            throw new IOException("File server is not available. Please ensure the TCP file server is running on port " + tcpPort, e);
        } catch (IOException e) {
            log.error("Error uploading file {}: {}", filename, e.getMessage(), e);
            throw new IOException("Failed to upload file: " + e.getMessage(), e);
        }
    }
}
