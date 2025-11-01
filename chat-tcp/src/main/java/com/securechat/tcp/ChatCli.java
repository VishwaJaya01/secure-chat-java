package com.securechat.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Placeholder command line interface for interacting with the TCP server.
 */
public class ChatCli {
    private static final Logger log = LoggerFactory.getLogger(ChatCli.class);

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9000;
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            log.info("Connected to {}:{}", host, port);
            log.info("Server says: {}", reader.readLine());
        }
    }
}
