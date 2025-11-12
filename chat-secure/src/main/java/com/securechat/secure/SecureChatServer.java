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
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize SSLContext", e);
        }
    }

    @Override
    public void run() {
        try {
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) factory.createServerSocket(port);
            log.info("TLS Chat Server started on port {}", port);
            
            while (running) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    clientSocket.startHandshake();
                    
                    String clientId = "client-" + clientCounter.incrementAndGet();
                    log.info("New TLS client connected: {} from {}", clientId, clientSocket.getRemoteSocketAddress());
                    
                    TlsChatClientHandler handler = new TlsChatClientHandler(clientSocket, clientId, messageHandler);
                    clientPool.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not start TLS Chat Server on port {}", port, e);
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
        log.info("TLS Chat Server shut down");
    }
}
