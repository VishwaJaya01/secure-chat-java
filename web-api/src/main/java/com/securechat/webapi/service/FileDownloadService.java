package com.securechat.webapi.service;

import com.securechat.tcp.FileDownloadClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

@Service
public class FileDownloadService {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadService.class);

    public void downloadFile(String filename, String tcpHost, int tcpPort, OutputStream outputStream) throws IOException {
        log.info("Downloading file {} from {}:{}", filename, tcpHost, tcpPort);
        FileDownloadClient client = new FileDownloadClient(tcpHost, tcpPort);
        client.downloadFile(filename, outputStream);
        log.info("File {} downloaded successfully", filename);
    }
}

