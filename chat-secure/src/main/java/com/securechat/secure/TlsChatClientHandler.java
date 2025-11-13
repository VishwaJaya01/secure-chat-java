package com.securechat.secure;

import com.securechat.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
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
            // Log TLS session details
            javax.net.ssl.SSLSession session = socket.getSession();
            log.debug("🔒 TLS: Session ID: {}",
                session.getId() != null ? bytesToHex(session.getId()) : "null");
            int peerCertCount = 0;
            try {
                var peerCerts = session.getPeerCertificates();
                peerCertCount = peerCerts != null ? peerCerts.length : 0;
            } catch (SSLPeerUnverifiedException ex) {
                log.debug("🔒 TLS: Client {} did not present certificates (mutual TLS not required)", clientId);
            }
            log.debug("🔒 TLS: Peer certificates: {}", peerCertCount);
            
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
                    log.info("🔒 TLS: Client {} identified as: {} (encrypted connection)", clientId, username);
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
                    log.info("🔒 TLS: Encrypted message received from {}: {}", username, content);
                    messageBroadcaster.accept(message);
                    writer.write("OK: Message sent");
                    writer.newLine();
                    writer.flush();
                } else if (line.equals("QUIT")) {
                    log.info("🔒 TLS: Client {} ({}) disconnected (TLS session closed)", clientId, username);
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
            log.warn("🔒 TLS: Error handling client {}: {}", clientId, e.getMessage());
        } finally {
            try {
                socket.close();
                log.debug("🔒 TLS: Socket closed for client {}", clientId);
            } catch (IOException e) {
                log.error("🔒 TLS: Error closing socket for client {}", clientId, e);
            }
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "empty";
        }
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 8); i++) {
            hex.append(String.format("%02x", bytes[i]));
        }
        return hex.toString() + (bytes.length > 8 ? "..." : "");
    }
}


