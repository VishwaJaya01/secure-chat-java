package com.securechat.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class FileSender implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FileSender.class);
    private static final int CHUNK_SIZE = 8192;

    private final Socket clientSocket;
    private final File file;

    public FileSender(Socket clientSocket, File file) {
        this.clientSocket = clientSocket;
        this.file = file;
    }

    @Override
    public void run() {
        try (
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            FileInputStream fis = new FileInputStream(file)
        ) {
            out.writeLong(file.length());
            
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            long totalBytesSent = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                ChunkFramer.writeChunk(out, chunk);
                totalBytesSent += bytesRead;
            }
            
            log.info("File {} sent successfully ({} bytes)", file.getName(), totalBytesSent);
        } catch (IOException e) {
            log.error("Error sending file {}", file.getName(), e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Error closing client socket", e);
            }
        }
    }
}

