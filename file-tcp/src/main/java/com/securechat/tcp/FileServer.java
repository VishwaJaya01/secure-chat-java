package com.securechat.tcp;

import com.securechat.core.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileServer implements Runnable, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FileServer.class);
    private static final String DOWNLOAD_COMMAND = "DOWNLOAD";

    private final int port;
    private final ExecutorService pool;
    private final Consumer<FileMetadata> onComplete;
    private final Function<String, File> fileResolver;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public FileServer(int port, Consumer<FileMetadata> onComplete) {
        this(port, onComplete, FileServer::defaultFileResolver);
    }

    public FileServer(int port, Consumer<FileMetadata> onComplete, Function<String, File> fileResolver) {
        this.port = port;
        this.onComplete = onComplete;
        this.fileResolver = fileResolver;
        this.pool = Executors.newCachedThreadPool();
    }

    private static File defaultFileResolver(String filename) {
        Path filesDir = Paths.get("./files");
        try {
            Files.createDirectories(filesDir);
        } catch (IOException e) {
            log.warn("Could not create files directory: {}", e.getMessage());
        }
        return filesDir.resolve(filename).toFile();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("FileServer started on port {}", port);
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    pool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not start FileServer on port {}", port, e);
        } finally {
            pool.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            // Peek at first message to determine if it's upload or download
            String firstMessage = in.readUTF();
            
            if (DOWNLOAD_COMMAND.equals(firstMessage)) {
                // Download request
                String filename = in.readUTF();
                File file = fileResolver.apply(filename);
                log.info("Download request for: {}, checking path: {}", filename, file.getAbsolutePath());
                if (file.exists() && file.isFile()) {
                    log.info("Serving download request for: {} ({} bytes)", filename, file.length());
                    // FileSender will handle closing the socket
                    new FileSender(clientSocket, file).run();
                    return; // Socket closed by FileSender
                } else {
                    log.warn("File not found for download: {} at path: {}", filename, file.getAbsolutePath());
                    // Try current directory as fallback (for files uploaded before the change)
                    File fallbackFile = new File(filename);
                    if (fallbackFile.exists() && fallbackFile.isFile()) {
                        log.info("Found file in current directory, serving: {}", filename);
                        new FileSender(clientSocket, fallbackFile).run();
                        return;
                    }
                }
            } else {
                // Upload request (backward compatible)
                String fileName = firstMessage;
                long fileSize = in.readLong();
                String owner = in.readUTF();
                String senderHost = clientSocket.getInetAddress().getHostAddress();
                String tcpHost = clientSocket.getLocalAddress().getHostAddress();
                int tcpPort = clientSocket.getLocalPort();

                log.info("Receiving file: {} ({} bytes) from {} (remote: {})", fileName, fileSize, owner, senderHost);

                File destinationFile = fileResolver.apply(fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(destinationFile)) {
                    long bytesRead = 0;
                    while (bytesRead < fileSize) {
                        byte[] chunk = ChunkFramer.readChunk(in);
                        fos.write(chunk);
                        bytesRead += chunk.length;
                    }
                }

                FileMetadata metadata = FileMetadata.create(fileName, fileSize, owner, tcpHost, tcpPort);
                log.info("File {} received successfully; announcing availability at {}:{}", fileName, tcpHost, tcpPort);
                onComplete.accept(metadata);
            }
        } catch (IOException e) {
            log.error("Error handling client connection", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                log.error("Error closing client socket", e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        pool.shutdownNow();
        log.info("FileServer shut down");
    }
}
