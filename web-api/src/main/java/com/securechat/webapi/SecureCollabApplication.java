package com.securechat.webapi;

import com.securechat.secure.SecureChatServer;
import com.securechat.tcp.FileServer;
import com.securechat.udp.PresenceClient;
import com.securechat.udp.PresenceServer;
import com.securechat.webapi.service.PresenceService;
import com.securechat.webapi.service.TlsChatService;
import com.securechat.webapi.store.FileMetaStore;
import com.securechat.webapi.telemetry.NetworkServiceKeys;
import com.securechat.webapi.telemetry.NetworkServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableAsync
public class SecureCollabApplication implements CommandLineRunner, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SecureCollabApplication.class);

    private FileServer fileServer;
    private SecureChatServer tlsChatServer;
    private PresenceServer udpPresenceServer;
    private PresenceClient udpPresenceClient;
    private final FileMetaStore fileMetaStore;
    private final TlsChatService tlsChatService;
    private final PresenceService presenceService;
    private final NetworkServiceRegistry networkServiceRegistry;
    private final ResourceLoader resourceLoader;

    @Value("${tls.chat.enabled:false}")
    private boolean tlsChatEnabled;

    @Value("${tls.chat.embedded:true}")
    private boolean tlsChatEmbedded;

    @Value("${tls.chat.port:9443}")
    private int tlsChatPort;

    @Value("${tls.chat.host:localhost}")
    private String tlsChatHost;

    @Value("${tls.chat.keystore.path:./certs/server-keystore.jks}")
    private String keystorePath;

    @Value("${tls.chat.keystore.password:changeit}")
    private String keystorePassword;

    @Value("${udp.presence.enabled:false}")
    private boolean udpPresenceEnabled;

    @Value("${udp.presence.port:9090}")
    private int udpPresencePort;

    @Value("${udp.presence.broadcastInterval:5000}")
    private long udpPresenceBroadcastInterval;

    @Value("${udp.presence.telemetry.log-every:1}")
    private int udpPresenceTelemetryLogEvery;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${file.transfer.tcp.port:6000}")
    private int fileServerPort;

    @Value("${file.transfer.tcp.host:localhost}")
    private String fileServerHost;

    @Value("${file.transfer.enabled:true}")
    private boolean fileTransferEnabled;

    @Value("${file.transfer.embedded:true}")
    private boolean fileTransferEmbedded;

    public SecureCollabApplication(FileMetaStore fileMetaStore,
                                   TlsChatService tlsChatService,
                                   PresenceService presenceService,
                                   NetworkServiceRegistry networkServiceRegistry,
                                   ResourceLoader resourceLoader) {
        this.fileMetaStore = fileMetaStore;
        this.tlsChatService = tlsChatService;
        this.presenceService = presenceService;
        this.networkServiceRegistry = networkServiceRegistry;
        this.resourceLoader = resourceLoader;
    }

    public static void main(String[] args) {
        SpringApplication.run(SecureCollabApplication.class, args);
    }

    @Bean
    public FileServer fileServer() {
        return new FileServer(fileServerPort, metadata -> {
            fileMetaStore.save(metadata);
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.TCP_FILE_SERVER,
                "UPLOAD",
                String.format("Stored metadata for %s (%d bytes) via tcp://%s:%d", metadata.filename(), metadata.fileSize(), metadata.tcpHost(), metadata.tcpPort())
            );
        });
    }

    @Override
    public void run(String... args) throws Exception {
        networkServiceRegistry.registerService(
            NetworkServiceKeys.TCP_FILE_SERVER,
            "TCP File Transfer Server",
            "TCP",
            "localhost:" + fileServerPort,
            "Streams file uploads/downloads for chat attachments"
        );
        if (!fileTransferEnabled) {
            networkServiceRegistry.serviceDisabled(NetworkServiceKeys.TCP_FILE_SERVER, "file.transfer.enabled=false");
        } else if (fileTransferEmbedded) {
            networkServiceRegistry.serviceStarting(NetworkServiceKeys.TCP_FILE_SERVER, "Spawning file transfer listener thread");
            fileServer = fileServer();
            Thread fileServerThread = new Thread(fileServer);
            fileServerThread.setName("TCP-FileServer");
            fileServerThread.start();
            networkServiceRegistry.serviceRunning(NetworkServiceKeys.TCP_FILE_SERVER, "Accepting TCP clients on port " + fileServerPort);
        } else {
            networkServiceRegistry.serviceRunning(
                NetworkServiceKeys.TCP_FILE_SERVER,
                "Expecting external FileServer at " + fileServerHost + ":" + fileServerPort
            );
            log.info("File transfer embedded server disabled; expecting external instance on {}:{}",
                fileServerHost, fileServerPort);
        }

        // Start TLS Chat Server if enabled
        networkServiceRegistry.registerService(
            NetworkServiceKeys.TLS_CHAT_SERVER,
            "TLS Chat Server",
            "TLS",
            "localhost:" + tlsChatPort,
            "Adds end-to-end encryption for chat delivery"
        );
        if (!tlsChatEnabled) {
            System.out.println("🔒 TLS: TLS Chat Server is disabled (tls.chat.enabled=false)");
            networkServiceRegistry.serviceDisabled(NetworkServiceKeys.TLS_CHAT_SERVER, "tls.chat.enabled=false");
        } else if (tlsChatEmbedded) {
            networkServiceRegistry.serviceStarting(NetworkServiceKeys.TLS_CHAT_SERVER, "Preparing keystore: " + keystorePath);
            System.out.println("🔒 TLS: Starting TLS Chat Server on port " + tlsChatPort);
            try (InputStream keystoreStream = openKeystoreStream()) {
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
                networkServiceRegistry.serviceRunning(NetworkServiceKeys.TLS_CHAT_SERVER, "TLS listener bound to port " + tlsChatPort);
            } catch (Exception e) {
                System.err.println("🔒 TLS: Failed to start TLS Chat Server: " + e.getMessage());
                System.err.println("🔒 TLS: TLS Chat will be disabled. Ensure keystore exists at: " + keystorePath);
                e.printStackTrace();
                networkServiceRegistry.serviceFailed(NetworkServiceKeys.TLS_CHAT_SERVER, "Failed to initialize: " + e.getMessage());
            }
        } else {
            System.out.println("🔒 TLS: Embedded TLS server disabled; expecting external instance at " + tlsChatHost + ":" + tlsChatPort);
            networkServiceRegistry.serviceRunning(
                NetworkServiceKeys.TLS_CHAT_SERVER,
                "Expecting external TLS server at " + tlsChatHost + ":" + tlsChatPort
            );
        }

        // Start UDP Presence Server and Client if enabled
        networkServiceRegistry.registerService(
            NetworkServiceKeys.UDP_PRESENCE_SERVER,
            "UDP Presence Beacon",
            "UDP",
            "0.0.0.0:" + udpPresencePort,
            "Broadcasts server availability to LAN peers"
        );
        networkServiceRegistry.registerService(
            NetworkServiceKeys.UDP_PRESENCE_CLIENT,
            "UDP Presence Listener",
            "UDP",
            "0.0.0.0:" + udpPresencePort,
            "Receives LAN presence beacons and updates the roster"
        );
        if (udpPresenceEnabled) {
            String serverId = "server-" + serverPort;
            networkServiceRegistry.serviceStarting(NetworkServiceKeys.UDP_PRESENCE_SERVER, "Bootstrapping presence broadcasts as " + serverId);
            udpPresenceServer = new PresenceServer(udpPresencePort, serverId, udpPresenceBroadcastInterval);
            Thread udpServerThread = new Thread(udpPresenceServer);
            udpServerThread.setName("UDP-Presence-Server");
            udpServerThread.start();
            networkServiceRegistry.serviceRunning(NetworkServiceKeys.UDP_PRESENCE_SERVER, "Broadcasting '" + serverId + "' on UDP port " + udpPresencePort + " every " + udpPresenceBroadcastInterval + "ms");
            
            // Listen for presence beacons from other servers
            networkServiceRegistry.serviceStarting(NetworkServiceKeys.UDP_PRESENCE_CLIENT, "Listening for peers on UDP port " + udpPresencePort);
            AtomicLong beaconLogCounter = new AtomicLong(0);
            udpPresenceClient = new PresenceClient(udpPresencePort, (serverIdFromBeacon) -> {
                // Update presence when we receive a beacon (displayName=null indicates UDP beacon)
                // This will only log when status changes (e.g., when member logs in)
                long sampleIndex = beaconLogCounter.incrementAndGet();
                if (shouldLogUdpTelemetry(sampleIndex)) {
                    networkServiceRegistry.recordUsage(
                        NetworkServiceKeys.UDP_PRESENCE_CLIENT,
                        "BEACON",
                        "Detected " + serverIdFromBeacon + " via UDP port " + udpPresencePort + " (sample #" + sampleIndex + ")"
                    );
                }
                presenceService.updatePresence(serverIdFromBeacon, null);
            });
            Thread udpClientThread = new Thread(udpPresenceClient);
            udpClientThread.setName("UDP-Presence-Client");
            udpClientThread.start();
            networkServiceRegistry.serviceRunning(NetworkServiceKeys.UDP_PRESENCE_CLIENT, "Receiving UDP beacons on port " + udpPresencePort);
        } else {
            networkServiceRegistry.serviceDisabled(NetworkServiceKeys.UDP_PRESENCE_SERVER, "udp.presence.enabled=false");
            networkServiceRegistry.serviceDisabled(NetworkServiceKeys.UDP_PRESENCE_CLIENT, "udp.presence.enabled=false");
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

    private boolean shouldLogUdpTelemetry(long sampleIndex) {
        return udpPresenceTelemetryLogEvery <= 1 || sampleIndex % udpPresenceTelemetryLogEvery == 0;
    }

    private InputStream openKeystoreStream() throws Exception {
        Resource resource = resourceLoader.getResource(keystorePath);
        if (!resource.exists() && !keystorePath.startsWith("classpath:")) {
            Path resolved = Paths.get(keystorePath).toAbsolutePath().normalize();
            resource = resourceLoader.getResource("file:" + resolved);
        }
        if (!resource.exists()) {
            throw new FileNotFoundException("Keystore not found at " + keystorePath);
        }
        System.out.println("🔒 TLS: Loading keystore from: " + resource.getDescription());
        return resource.getInputStream();
    }
}
