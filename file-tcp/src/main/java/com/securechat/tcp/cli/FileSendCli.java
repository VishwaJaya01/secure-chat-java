package com.securechat.tcp.cli;

import com.securechat.tcp.FileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class FileSendCli {

    private static final Logger log = LoggerFactory.getLogger(FileSendCli.class);

    private FileSendCli() {
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: FileSendCli <tcp-host> <tcp-port> <file-path> <owner>");
            System.exit(1);
            return;
        }

        String host = args[0];
        int port = parsePort(args[1]);
        File file = new File(args[2]);
        String owner = args[3];

        if (!file.exists() || !file.isFile()) {
            System.err.printf("File not found: %s%n", file.getAbsolutePath());
            System.exit(2);
        }

        try {
            new FileClient(host, port).sendFile(file, owner);
            log.info("File transfer completed");
        } catch (Exception e) {
            log.error("File transfer failed", e);
            System.exit(3);
        }
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port value: " + value, e);
        }
    }
}

