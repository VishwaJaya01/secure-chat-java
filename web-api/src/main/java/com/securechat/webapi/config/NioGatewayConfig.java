package com.securechat.webapi.config;

import com.securechat.announce.nio.AnnouncementGateway;
import com.securechat.core.AnnouncementBroadcastHub;
import com.securechat.webapi.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.function.Consumer;

@Configuration
public class NioGatewayConfig {
    private static final Logger log = LoggerFactory.getLogger(NioGatewayConfig.class);

    @Value("${nio.gateway.port:6001}")
    private int gatewayPort;

    @Value("${nio.gateway.enabled:true}")
    private boolean gatewayEnabled;

    private AnnouncementGateway gateway;

    @Bean
    public AnnouncementBroadcastHub announcementBroadcastHub() {
        return new AnnouncementBroadcastHub();
    }

    @Bean
    public AnnouncementGateway announcementGateway(AnnouncementBroadcastHub broadcastHub, @Lazy AnnouncementService announcementService) {
        if (!gatewayEnabled) {
            log.info("NIO Gateway is disabled");
            return null;
        }

        // This consumer processes messages received from the NIO gateway
        Consumer<String> nioMessageConsumer = (message) -> {
            try {
                log.info("Processing message from NIO client: {}", message);
                String[] parts = message.split("\\|", 4);
                if (parts.length == 4 && "POST".equalsIgnoreCase(parts[0])) {
                    String author = parts[1];
                    String title = parts[2];
                    String content = parts[3];
                    // Use the service to create the announcement
                    // This ensures it's added to the store and broadcast to all clients (SSE and back to NIO)
                    announcementService.createAnnouncement(author, title, content);
                } else {
                    log.warn("Received malformed message from NIO client: {}", message);
                }
            } catch (Exception e) {
                log.error("Error processing message from NIO client", e);
            }
        };

        try {
            gateway = new AnnouncementGateway(gatewayPort, broadcastHub, nioMessageConsumer);
            gateway.start();
            log.info("NIO AnnouncementGateway started on port {}", gatewayPort);

            // Register the gateway's broadcast method as a listener on the hub.
            // Now, when hub.broadcast() is called, the gateway will send the message to all connected NIO clients.
            broadcastHub.register(gateway::broadcast);
            log.info("NIO Gateway registered to receive broadcasts from the hub");

            return gateway;
        } catch (IOException e) {
            log.error("Failed to start NIO Gateway", e);
            throw new RuntimeException("Failed to start NIO Gateway", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (gateway != null) {
            try {
                gateway.close();
                log.info("NIO Gateway shut down");
            } catch (IOException e) {
                log.error("Error shutting down NIO Gateway", e);
            }
        }
    }
}
