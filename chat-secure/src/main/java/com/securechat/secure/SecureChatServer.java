package com.securechat.secure;

import com.securechat.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * TLS-enabled chat server that accepts secure connections and handles chat messages.
 */
public class SecureChatServer implements Runnable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(SecureChatServer.class);
    
    private final int port;
    private final SSLContext sslContext;
    private final Consumer<Message> messageHandler;
    private final ExecutorService clientPool;
    private final AtomicInteger clientCounter = new AtomicInteger(0);
    private volatile boolean running = true;
    private SSLServerSocket serverSocket;

    public SecureChatServer(int port, Consumer<Message> messageHandler, InputStream keyStoreStream, char[] password) {
        this.port = port;
        this.messageHandler = messageHandler;
        this.sslContext = createContext(keyStoreStream, password);
        this.clientPool = Executors.newCachedThreadPool();
    }

    private SSLContext createContext(InputStream keyStoreStream, char[] password) {
        try {
            log.info("🔒 TLS: Initializing SSLContext with JKS keystore");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, password);
            log.debug("🔒 TLS: Keystore loaded successfully");
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
            log.debug("🔒 TLS: KeyManagerFactory initialized with algorithm: {}", KeyManagerFactory.getDefaultAlgorithm());
            
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            log.info("🔒 TLS: SSLContext initialized with TLS protocol");
            return context;
        } catch (Exception e) {
            log.error("🔒 TLS: Failed to initialize SSLContext", e);
            throw new IllegalStateException("Unable to initialize SSLContext", e);
        }
    }

    @Override
    public void run() {
        try {
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) factory.createServerSocket(port);
            
            // Log enabled cipher suites
            String[] enabledCiphers = serverSocket.getEnabledCipherSuites();
            String[] enabledProtocols = serverSocket.getEnabledProtocols();
            log.info("🔒 TLS Chat Server started on port {}", port);
            log.info("🔒 TLS: Enabled cipher suites ({}): {}", enabledCiphers.length, 
                String.join(", ", Arrays.copyOf(enabledCiphers, Math.min(3, enabledCiphers.length))) + 
                (enabledCiphers.length > 3 ? "..." : ""));
            log.info("🔒 TLS: Enabled protocols: {}", String.join(", ", enabledProtocols));
            log.info("🔒 TLS: Server ready to accept secure connections on port {}", port);
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("🔒 TLS CHAT SERVER IS RUNNING");
            System.out.println("   Port: " + port);
            System.out.println("   Protocols: " + String.join(", ", enabledProtocols));
            System.out.println("   Status: Waiting for TLS client connections...");
            System.out.println("   To test: Connect a TLS client to localhost:" + port);
            System.out.println("═══════════════════════════════════════════════════════════");
            
            while (running) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    String clientId = "client-" + clientCounter.incrementAndGet();
                    log.info("🔒 TLS: Incoming connection from {} (client: {})", 
                        clientSocket.getRemoteSocketAddress(), clientId);
                    log.debug("🔒 TLS: Starting handshake for client {}", clientId);
                    
                    // Start TLS handshake
                    clientSocket.startHandshake();
                    
                    // Log TLS connection details after handshake
                    String protocol = clientSocket.getSession().getProtocol();
                    String cipherSuite = clientSocket.getSession().getCipherSuite();
                    log.info("🔒 TLS HANDSHAKE COMPLETE: Client {} connected", clientId);
                    log.info("🔒 TLS: Protocol: {}, Cipher Suite: {}", protocol, cipherSuite);
                    log.info("🔒 TLS: Connection encrypted and authenticated");
                    
                    TlsChatClientHandler handler = new TlsChatClientHandler(clientSocket, clientId, messageHandler);
                    clientPool.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        log.error("🔒 TLS: Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("🔒 TLS: Could not start TLS Chat Server on port {}", port, e);
        } finally {
            clientPool.shutdown();
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        clientPool.shutdownNow();
        log.info("🔒 TLS Chat Server shut down");
    }
}
