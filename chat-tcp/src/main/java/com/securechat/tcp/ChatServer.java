package com.securechat.tcp;

import com.securechat.core.BroadcastHub;
import com.securechat.core.UserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Placeholder TCP chat server wiring together basic components.
 */
public class ChatServer {
    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);
    private final int port;
    private final BroadcastHub hub;
    private final UserRegistry users;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatServer(int port, BroadcastHub hub, UserRegistry users) {
        this.port = port;
        this.hub = hub;
        this.users = users;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Starting placeholder TCP chat server on port {}", port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                executor.submit(new ClientHandler(client, hub, users));
            }
        }
    }
}
