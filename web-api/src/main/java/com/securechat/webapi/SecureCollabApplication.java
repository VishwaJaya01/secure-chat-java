package com.securechat.webapi;

import com.securechat.secure.SecureChatServer;
import com.securechat.tcp.FileServer;
import com.securechat.udp.PresenceClient;
import com.securechat.udp.PresenceServer;
import com.securechat.webapi.service.PresenceService;
import com.securechat.webapi.service.TlsChatService;
import com.securechat.webapi.store.FileMetaStore;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.FileInputStream;
import java.io.InputStream;

@SpringBootApplication
@EnableAsync
public class SecureCollabApplication implements CommandLineRunner, DisposableBean {

    private FileServer fileServer;
    private SecureChatServer tlsChatServer;
    private PresenceServer udpPresenceServer;
    private PresenceClient udpPresenceClient;
    private final FileMetaStore fileMetaStore;
    private final TlsChatService tlsChatService;
    private final PresenceService presenceService;

    @Value("${tls.chat.enabled:false}")
    private boolean tlsChatEnabled;

    @Value("${tls.chat.port:9443}")
    private int tlsChatPort;

    @Value("${tls.chat.keystore.path:./certs/server-keystore.jks}")
    private String keystorePath;

    @Value("${tls.chat.keystore.password:changeit}")
    private String keystorePassword;

    @Value("${udp.presence.enabled:false}")
    private boolean udpPresenceEnabled;

    @Value("${udp.presence.port:9090}")
    private int udpPresencePort;

    @Value("${server.port:8080}")
    private int serverPort;

    public SecureCollabApplication(FileMetaStore fileMetaStore, TlsChatService tlsChatService, PresenceService presenceService) {
        this.fileMetaStore = fileMetaStore;
        this.tlsChatService = tlsChatService;
        this.presenceService = presenceService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SecureCollabApplication.class, args);
    }

    @Bean
    public FileServer fileServer() {
        return new FileServer(6000, metadata -> {
            fileMetaStore.save(metadata);
        });
    }

    @Override
    public void run(String... args) throws Exception {
        // Start File Server
        fileServer = fileServer();
        new Thread(fileServer).start();

        // Start TLS Chat Server if enabled
        if (tlsChatEnabled) {
            System.out.println("🔒 TLS: Starting TLS Chat Server on port " + tlsChatPort);
            System.out.println("🔒 TLS: Loading keystore from: " + keystorePath);
            try (InputStream keystoreStream = new FileInputStream(keystorePath)) {
                char[] password = keystorePassword.toCharArray();
                tlsChatServer = new SecureChatServer(
                    tlsChatPort,
                    tlsChatService::handleTlsMessage,
                    keystoreStream,
                    password
                );
                Thread tlsThread = new Thread(tlsChatServer);
                tlsThread.setName("TLS-Chat-Server");
                tlsThread.start();
                System.out.println("🔒 TLS: TLS Chat Server thread started");
            } catch (Exception e) {
                System.err.println("🔒 TLS: Failed to start TLS Chat Server: " + e.getMessage());
                System.err.println("🔒 TLS: TLS Chat will be disabled. Ensure keystore exists at: " + keystorePath);
                e.printStackTrace();
            }
        } else {
            System.out.println("🔒 TLS: TLS Chat Server is disabled (tls.chat.enabled=false)");
        }

        // Start UDP Presence Server and Client if enabled
        if (udpPresenceEnabled) {
            String serverId = "server-" + serverPort;
            udpPresenceServer = new PresenceServer(udpPresencePort, serverId);
            new Thread(udpPresenceServer).start();
            
            // Listen for presence beacons from other servers
            udpPresenceClient = new PresenceClient(udpPresencePort, (serverIdFromBeacon) -> {
                // Update presence when we receive a beacon (displayName=null indicates UDP beacon)
                // This will only log when status changes (e.g., when member logs in)
                presenceService.updatePresence(serverIdFromBeacon, null);
            });
            new Thread(udpPresenceClient).start();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (fileServer != null) {
            fileServer.close();
        }
        if (tlsChatServer != null) {
            tlsChatServer.close();
        }
        if (udpPresenceServer != null) {
            udpPresenceServer.close();
        }
        if (udpPresenceClient != null) {
            udpPresenceClient.close();
        }
    }
}


