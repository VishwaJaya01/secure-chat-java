package com.securechat.secure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Scanner;

/**
 * TLS chat client CLI to connect to the secure chat server.
 * 
 * Usage:
 *   java -cp ... SecureChatCli --host=localhost --port=9443 --user=alice [--truststore=path] [--storepass=password]
 * 
 * Commands:
 *   USER <username> - Identify yourself
 *   MSG <text>      - Send a message
 *   QUIT            - Disconnect
 */
public class SecureChatCli {
    private static final Logger log = LoggerFactory.getLogger(SecureChatCli.class);

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 9443;
        String truststoreLocation = null;
        String truststoreType = "PKCS12";
        char[] password = "changeit".toCharArray();
        String user = "guest";

        for (String arg : args) {
            if (arg.startsWith("--host=")) {
                host = arg.substring("--host=".length());
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--truststore=")) {
                truststoreLocation = arg.substring("--truststore=".length());
            } else if (arg.startsWith("--storepass=")) {
                Arrays.fill(password, '\0');
                password = arg.substring("--storepass=".length()).toCharArray();
            } else if (arg.startsWith("--type=")) {
                truststoreType = arg.substring("--type=".length());
            } else if (arg.startsWith("--user=")) {
                user = arg.substring("--user=".length());
            }
        }

        SSLSocketFactory factory = createSocketFactory(truststoreLocation, password.clone(), truststoreType);
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             Scanner scanner = new Scanner(System.in)) {
            
            socket.startHandshake();
            log.info("Connected to secure chat at {}:{}", host, port);
            
            // Start a thread to read server responses
            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Server] " + line);
                        if (line.equals("BYE")) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        log.error("Error reading from server", e);
                    }
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();
            
            // Read welcome message
            String greeting = reader.readLine();
            if (greeting != null) {
                System.out.println("[Server] " + greeting);
            }
            
            // Send USER command
            writer.write("USER " + user);
            writer.newLine();
            writer.flush();
            
            // Interactive loop
            System.out.println("Type commands (USER <name>, MSG <text>, QUIT):");
            while (true) {
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    writer.write("QUIT");
                    writer.newLine();
                    writer.flush();
                    break;
                }
                
                writer.write(input);
                writer.newLine();
                writer.flush();
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static SSLSocketFactory createSocketFactory(String truststoreLocation, char[] password, String truststoreType) {
        if (truststoreLocation == null) {
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        Path path = Path.of(truststoreLocation);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Truststore not found at " + path.toAbsolutePath());
        }

        try (InputStream in = Files.newInputStream(path)) {
            KeyStore trustStore = KeyStore.getInstance(truststoreType);
            trustStore.load(in, password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialise SSL context from truststore", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
