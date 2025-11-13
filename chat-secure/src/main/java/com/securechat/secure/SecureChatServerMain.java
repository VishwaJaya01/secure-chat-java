package com.securechat.secure;

import com.securechat.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Standalone launcher for the TLS chat server so it can run from its own terminal.
 */
public final class SecureChatServerMain {
    private static final Logger log = LoggerFactory.getLogger(SecureChatServerMain.class);

    private SecureChatServerMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = 9443;
        String keystoreLocation = "classpath:certs/server-keystore.jks";
        char[] password = "changeit".toCharArray();

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--keystore=")) {
                keystoreLocation = arg.substring("--keystore=".length());
            } else if (arg.startsWith("--storepass=")) {
                Arrays.fill(password, '\0');
                password = arg.substring("--storepass=".length()).toCharArray();
            }
        }

        byte[] keystoreBytes = loadKeystoreBytes(keystoreLocation);
        SecureChatServer server = new SecureChatServer(
                port,
                SecureChatServerMain::logInboundMessage,
                new ByteArrayInputStream(keystoreBytes),
                password.clone()
        );
        Arrays.fill(password, '\0');

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (IOException e) {
                log.warn("Failed to stop TLS chat server cleanly", e);
            }
        }, "tls-chat-server-shutdown"));

        log.info("───────────────────────────────────────────────────────────────");
        log.info("Launching standalone TLS chat server on port {}", port);
        log.info("Use SecureChatCli or the web-api to connect.");
        log.info("Press CTRL+C to stop.");
        log.info("───────────────────────────────────────────────────────────────");
        server.run();
    }

    private static byte[] loadKeystoreBytes(String location) throws IOException {
        if (location.startsWith("classpath:")) {
            String resource = location.substring("classpath:".length());
            try (InputStream in = SecureChatServerMain.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Keystore not found in classpath at " + resource);
                }
                return in.readAllBytes();
            }
        }
        Path path = Path.of(location);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Keystore not found at " + path.toAbsolutePath());
        }
        return Files.readAllBytes(path);
    }

    private static void logInboundMessage(Message message) {
        log.info("💬 [{}] {}", message.getFrom(), message.getContent());
    }
}
