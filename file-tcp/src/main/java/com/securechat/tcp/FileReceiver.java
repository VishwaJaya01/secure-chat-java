package com.securechat.tcp;

import com.securechat.core.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class FileReceiver implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FileReceiver.class);

    private final Socket clientSocket;
    private final Consumer<FileMetadata> onComplete;

    public FileReceiver(Socket clientSocket, Consumer<FileMetadata> onComplete) {
        this.clientSocket = clientSocket;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        try (
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        ) {
            String fileName = in.readUTF();
            long fileSize = in.readLong();
            String owner = in.readUTF();
            String senderHost = clientSocket.getInetAddress().getHostAddress();
            String tcpHost = clientSocket.getLocalAddress().getHostAddress();
            int tcpPort = clientSocket.getLocalPort();

            log.info("Receiving file: {} ({} bytes) from {} (remote: {})", fileName, fileSize, owner, senderHost);

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                long bytesRead = 0;
                while (bytesRead < fileSize) {
                    byte[] chunk = ChunkFramer.readChunk(in);
                    fos.write(chunk);
                    bytesRead += chunk.length;
                    log.info("Received chunk for {}, {}/{} bytes", fileName, bytesRead, fileSize);
                }
            }

            FileMetadata metadata = FileMetadata.create(fileName, fileSize, owner, tcpHost, tcpPort);
            log.info("File {} received successfully; announcing availability at {}:{}", fileName, tcpHost, tcpPort);
            onComplete.accept(metadata);

        } catch (IOException e) {
            log.error("Error receiving file", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Error closing client socket", e);
            }
        }
    }
}
