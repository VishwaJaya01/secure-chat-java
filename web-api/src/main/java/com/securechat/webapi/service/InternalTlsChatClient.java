package com.securechat.webapi.service;

import com.securechat.webapi.telemetry.NetworkServiceKeys;
import com.securechat.webapi.telemetry.NetworkServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
    private final ResourceLoader resourceLoader;
    private final NetworkServiceRegistry networkServiceRegistry;
    private SSLContext sslContext;

    public InternalTlsChatClient(
            @Value("${tls.chat.host:localhost}") String tlsHost,
            @Value("${tls.chat.port:9443}") int tlsPort,
            @Value("${tls.chat.keystore.path:./certs/server-keystore.jks}") String keystorePath,
            @Value("${tls.chat.keystore.password:changeit}") String keystorePassword,
            NetworkServiceRegistry networkServiceRegistry,
            ResourceLoader resourceLoader) {
        this.tlsHost = tlsHost;
        this.tlsPort = tlsPort;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.networkServiceRegistry = networkServiceRegistry;
        this.resourceLoader = resourceLoader;
        networkServiceRegistry.registerService(
            NetworkServiceKeys.TLS_CHAT_CLIENT,
            "TLS Chat Router",
            "TLS",
            tlsHost + ":" + tlsPort,
            "Loops HTTP chat traffic back through the TLS server"
        );
        initializeSslContext();
    }

    private void initializeSslContext() {
        try {
            // Use the same keystore as the server (for self-signed certs)
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream in = resolveKeystoreStream()) {
                trustStore.load(in, keystorePassword.toCharArray());
            }
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            log.info("🔒 TLS: Internal TLS client initialized for routing HTTP messages through TLS");
            networkServiceRegistry.serviceRunning(
                NetworkServiceKeys.TLS_CHAT_CLIENT,
                "Trust store loaded from " + keystorePath
            );
        } catch (Exception e) {
            log.warn("🔒 TLS: Failed to initialize internal TLS client: {}. Messages will use HTTP only.", e.getMessage());
            sslContext = null;
            networkServiceRegistry.serviceDegraded(
                NetworkServiceKeys.TLS_CHAT_CLIENT,
                "Trust store initialization failed: " + e.getMessage()
            );
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
            networkServiceRegistry.serviceDegraded(
                NetworkServiceKeys.TLS_CHAT_CLIENT,
                "Attempted TLS routing without SSL context for user " + username
            );
            return;
        }

        try {
            SSLSocketFactory factory = sslContext.getSocketFactory();
            log.info("      → Creating TLS connection to {}:{}", tlsHost, tlsPort);
            log.info("      → Using SSLContext with TrustManagerFactory");
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.TLS_CHAT_CLIENT,
                "CONNECT",
                "Opening TLS socket to tls://" + tlsHost + ":" + tlsPort + " for " + username
            );
            
            try (SSLSocket socket = (SSLSocket) factory.createSocket(tlsHost, tlsPort);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                
                // Perform TLS handshake
                socket.startHandshake();
                String protocol = socket.getSession().getProtocol();
                String cipherSuite = socket.getSession().getCipherSuite();
                log.info("🔒 TLS HANDSHAKE COMPLETE: Internal connection established");
                log.info("🔒 TLS: Protocol: {}, Cipher Suite: {}", protocol, cipherSuite);
                networkServiceRegistry.recordUsage(
                    NetworkServiceKeys.TLS_CHAT_CLIENT,
                    "HANDSHAKE",
                    String.format("%s negotiated %s/%s on tls://%s:%d", username, protocol, cipherSuite, tlsHost, tlsPort)
                );
                networkServiceRegistry.recordUsage(
                    NetworkServiceKeys.TLS_CHAT_SERVER,
                    "HANDSHAKE",
                    String.format("Internal router connected as %s over port %d", username, tlsPort)
                );
                
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
                networkServiceRegistry.recordUsage(
                    NetworkServiceKeys.TLS_CHAT_CLIENT,
                    "CLOSE",
                    "TLS socket closed for " + username
                );
            }
        } catch (Exception e) {
            log.warn("🔒 TLS: Failed to route message through TLS: {}. Message will be processed via HTTP.", e.getMessage());
            networkServiceRegistry.serviceDegraded(
                NetworkServiceKeys.TLS_CHAT_CLIENT,
                "TLS routing failed for " + username + ": " + e.getMessage()
            );
        }
    }

    private InputStream resolveKeystoreStream() throws IOException {
        Resource resource = resourceLoader.getResource(keystorePath);
        if (!resource.exists() && !keystorePath.startsWith("classpath:")) {
            resource = resourceLoader.getResource("file:" + keystorePath);
        }
        if (!resource.exists()) {
            throw new FileNotFoundException("Keystore not found at " + keystorePath);
        }
        log.info("🔒 TLS: Loading client trust store from {}", resource.getDescription());
        return resource.getInputStream();
    }
}

