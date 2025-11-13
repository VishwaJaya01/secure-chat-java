package com.securechat.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileDownloadClient {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadClient.class);

    private final String host;
    private final int port;

    public FileDownloadClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void downloadFile(String filename, OutputStream outputStream) throws IOException {
        try (
            Socket socket = new Socket(host, port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            // Send download request
            out.writeUTF("DOWNLOAD");
            out.writeUTF(filename);
            out.flush();

            // Read file size
            long fileSize = in.readLong();
            log.info("Downloading file: {} ({} bytes)", filename, fileSize);

            // Read and write chunks
            long bytesRead = 0;
            while (bytesRead < fileSize) {
                byte[] chunk = ChunkFramer.readChunk(in);
                outputStream.write(chunk);
                bytesRead += chunk.length;
            }

            log.info("File {} downloaded successfully ({} bytes)", filename, bytesRead);
        }
    }

    public void downloadFile(String filename, File destinationFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            downloadFile(filename, fos);
        }
    }
}

