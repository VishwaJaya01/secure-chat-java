package com.securechat.tcp;

import com.securechat.core.BroadcastHub;
import com.securechat.core.UserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Placeholder client handler for TCP connections.
 */
public class ClientHandler implements Runnable, Closeable {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final BroadcastHub hub;
    private final UserRegistry users;

    public ClientHandler(Socket socket, BroadcastHub hub, UserRegistry users) {
        this.socket = socket;
        this.hub = hub;
        this.users = users;
    }

    @Override
    public void run() {
        log.info("Handling new TCP client: {}", socket.getRemoteSocketAddress());
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            out.write("Placeholder TCP handler\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.warn("Client handler exception", e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            log.debug("Error closing socket", e);
        }
    }
}
