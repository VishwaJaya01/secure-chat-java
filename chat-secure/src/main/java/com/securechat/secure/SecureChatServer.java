package com.securechat.secure;

import com.securechat.core.BroadcastHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Placeholder secure chat server using TLS sockets.
 */
public class SecureChatServer {
    private static final Logger log = LoggerFactory.getLogger(SecureChatServer.class);
    private final int port;
    private final BroadcastHub hub;
    private final SSLContext sslContext;

    public SecureChatServer(int port, BroadcastHub hub, InputStream keyStoreStream, char[] password) {
        this.port = port;
        this.hub = hub;
        this.sslContext = createContext(keyStoreStream, password);
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

    public void start() throws IOException {
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
            log.info("Starting placeholder secure chat server on port {}", port);
            serverSocket.accept();
        }
    }
}
