package com.securechat.secure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Placeholder CLI to connect to the secure chat server via TLS.
 */
public class SecureChatCli {
    private static final Logger log = LoggerFactory.getLogger(SecureChatCli.class);

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9443;
        SSLSocketFactory factory = SSLContext.getDefault().getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            socket.startHandshake();
            log.info("Connected to secure chat at {}:{}", host, port);
            log.info("Server says: {}", reader.readLine());
        }
    }
}
