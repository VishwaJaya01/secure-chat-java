package com.securechat.tcp;

import com.securechat.core.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Standalone launcher for the TCP file transfer server so it can run outside the Spring Boot app.
 */
public final class FileTcpServerMain {
    private static final Logger log = LoggerFactory.getLogger(FileTcpServerMain.class);

    private FileTcpServerMain() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 6000;
        Path storageDir = Paths.get("./files");
        String sseUrl = "http://localhost:8080/api/files/stream";
        boolean monitorOnly = false;
        boolean sseEnabled = true;

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--dir=")) {
                storageDir = Paths.get(arg.substring("--dir=".length()));
            } else if (arg.startsWith("--sse-url=")) {
                sseUrl = arg.substring("--sse-url=".length());
            } else if (arg.equalsIgnoreCase("--no-sse")) {
                sseEnabled = false;
            } else if (arg.equalsIgnoreCase("--monitor-only")) {
                monitorOnly = true;
            }
        }

        Files.createDirectories(storageDir);
        Path finalStorageDir = storageDir.toAbsolutePath().normalize();

        boolean portFree = monitorOnly ? false : isPortFree("0.0.0.0", port);
        if (!monitorOnly && portFree) {
            startTcpServer(port, finalStorageDir);
            return;
        }

        if (!sseEnabled) {
            log.error("Port {} is already in use and SSE bridge is disabled (use --sse-url or remove --no-sse).", port);
            return;
        }

        if (!monitorOnly) {
            log.info("Port {} already in use; entering mirror mode instead of starting another TCP listener.", port);
        } else {
            log.info("Monitor-only mode enabled; no TCP listener will be started.");
        }

        startSseBridge(sseUrl);
    }

    private static void startTcpServer(int port, Path storageDir) throws IOException {
        FileServer server = new FileServer(
                port,
                FileTcpServerMain::logTransferComplete,
                filename -> resolveStorageFile(storageDir, filename)
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (IOException e) {
                log.warn("Failed to stop TCP file server cleanly", e);
            }
        }, "file-tcp-server-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("TCP File Server listening on port {}", port);
        log.info("Files directory: {}", storageDir);
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");
        server.run();
    }

    private static void startSseBridge(String sseUrl) throws InterruptedException {
        FileTransferSseBridge bridge = new FileTransferSseBridge(sseUrl);
        bridge.start();
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bridge.close();
            latch.countDown();
        }, "file-tcp-sse-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("File Transfer SSE bridge watching {}", sseUrl);
        log.info("Uploads handled by the Spring Boot instance will be mirrored here.");
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");
        latch.await();
    }

    private static boolean isPortFree(String host, int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(host, port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File resolveStorageFile(Path storageDir, String filename) {
        Path resolved = storageDir.resolve(filename).normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        return resolved.toFile();
    }

    private static void logTransferComplete(FileMetadata metadata) {
        log.info("📁 File '{}' uploaded by {} ({} bytes) – available via {}:{}",
                metadata.filename(),
                metadata.owner(),
                metadata.fileSize(),
                metadata.tcpHost(),
                metadata.tcpPort());
    }
}
