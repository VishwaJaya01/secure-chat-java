package com.securechat.webapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.KeyStore;

/**
 * Internal TLS client that routes HTTP chat messages through TLS
 * to demonstrate TLS encryption for all messages.
 */
@Service
public class InternalTlsChatClient {
    private static final Logger log = LoggerFactory.getLogger(InternalTlsChatClient.class);
    
    private final String tlsHost;
    private final int tlsPort;
    private final String keystorePath;
    private final String keystorePassword;
    private SSLContext sslContext;

    public InternalTlsChatClient(
            @Value("${tls.chat.host:localhost}") String tlsHost,
            @Value("${tls.chat.port:9443}") int tlsPort,
            @Value("${tls.chat.keystore.path:./certs/server-keystore.jks}") String keystorePath,
            @Value("${tls.chat.keystore.password:changeit}") String keystorePassword) {
        this.tlsHost = tlsHost;
        this.tlsPort = tlsPort;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        initializeSslContext();
    }

    private void initializeSslContext() {
        try {
            // Use the same keystore as the server (for self-signed certs)
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                trustStore.load(fis, keystorePassword.toCharArray());
            }
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            log.info("🔒 TLS: Internal TLS client initialized for routing HTTP messages through TLS");
        } catch (Exception e) {
            log.warn("🔒 TLS: Failed to initialize internal TLS client: {}. Messages will use HTTP only.", e.getMessage());
            sslContext = null;
        }
    }

    /**
     * Routes a message through TLS to demonstrate TLS encryption.
     * This creates a TLS connection internally even for HTTP-originated messages.
     */
    public void sendMessageViaTls(String username, String content) {
        log.info("   └─ [SERVICE] InternalTlsChatClient.sendMessageViaTls() - Routing through TLS");
        
        if (sslContext == null) {
            log.warn("      → ⚠️  Cannot route message through TLS - SSL context not initialized");
            return;
        }

        try {
            SSLSocketFactory factory = sslContext.getSocketFactory();
            log.info("      → Creating TLS connection to {}:{}", tlsHost, tlsPort);
            log.info("      → Using SSLContext with TrustManagerFactory");
            
            try (SSLSocket socket = (SSLSocket) factory.createSocket(tlsHost, tlsPort);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                
                // Perform TLS handshake
                socket.startHandshake();
                String protocol = socket.getSession().getProtocol();
                String cipherSuite = socket.getSession().getCipherSuite();
                log.info("🔒 TLS HANDSHAKE COMPLETE: Internal connection established");
                log.info("🔒 TLS: Protocol: {}, Cipher Suite: {}", protocol, cipherSuite);
                
                // Read welcome message
                String welcome = reader.readLine();
                log.debug("🔒 TLS: Server welcome: {}", welcome);
                
                // Send USER command
                writer.write("USER " + username);
                writer.newLine();
                writer.flush();
                log.info("🔒 TLS: Client identified as: {} (encrypted connection)", username);
                
                // Read USER response
                String userResponse = reader.readLine();
                log.debug("🔒 TLS: User response: {}", userResponse);
                
                // Send MSG command
                writer.write("MSG " + content);
                writer.newLine();
                writer.flush();
                
                String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                log.info("🔒 TLS: Encrypted message sent from {}: {}", username, preview);
                
                // Read MSG response
                String msgResponse = reader.readLine();
                log.debug("🔒 TLS: Message response: {}", msgResponse);
                
                // Send QUIT
                writer.write("QUIT");
                writer.newLine();
                writer.flush();
                
                log.info("🔒 TLS: Internal TLS connection closed");
            }
        } catch (Exception e) {
            log.warn("🔒 TLS: Failed to route message through TLS: {}. Message will be processed via HTTP.", e.getMessage());
        }
    }
}

