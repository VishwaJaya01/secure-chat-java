package com.securechat.secure;

import com.securechat.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.function.Consumer;

public class TlsChatClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(TlsChatClientHandler.class);
    
    private final SSLSocket socket;
    private final String clientId;
    private final Consumer<Message> messageBroadcaster;
    private String username;

    public TlsChatClientHandler(SSLSocket socket, String clientId, Consumer<Message> messageBroadcaster) {
        this.socket = socket;
        this.clientId = clientId;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            // Send welcome message
            writer.write("Welcome to Secure Chat Server. Please identify yourself with: USER <username>");
            writer.newLine();
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (line.startsWith("USER ")) {
                    username = line.substring(5).trim();
                    if (username.isEmpty()) {
                        writer.write("ERROR: Username cannot be empty");
                        writer.newLine();
                        writer.flush();
                        continue;
                    }
                    log.info("Client {} identified as: {}", clientId, username);
                    writer.write("OK: You are now identified as " + username);
                    writer.newLine();
                    writer.flush();
                } else if (line.startsWith("MSG ")) {
                    if (username == null) {
                        writer.write("ERROR: Please identify yourself first with USER <username>");
                        writer.newLine();
                        writer.flush();
                        continue;
                    }
                    String content = line.substring(4).trim();
                    if (content.isEmpty()) {
                        writer.write("ERROR: Message cannot be empty");
                        writer.newLine();
                        writer.flush();
                        continue;
                    }
                    
                    Message message = new Message(username, content, Instant.now());
                    log.info("Received message from {}: {}", username, content);
                    messageBroadcaster.accept(message);
                    writer.write("OK: Message sent");
                    writer.newLine();
                    writer.flush();
                } else if (line.equals("QUIT")) {
                    log.info("Client {} ({}) disconnected", clientId, username);
                    writer.write("BYE");
                    writer.newLine();
                    writer.flush();
                    break;
                } else {
                    writer.write("ERROR: Unknown command. Use: USER <name>, MSG <text>, or QUIT");
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException e) {
            log.warn("Error handling client {}: {}", clientId, e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing socket for client {}", clientId, e);
            }
        }
    }
}

