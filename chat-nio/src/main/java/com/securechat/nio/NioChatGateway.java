package com.securechat.nio;

import com.securechat.core.BroadcastHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * Placeholder non-blocking gateway for multiplexing chat sessions.
 */
public class NioChatGateway implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(NioChatGateway.class);
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final BroadcastHub hub;

    public NioChatGateway(int port, BroadcastHub hub) throws IOException {
        this.hub = hub;
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void loopOnce() throws IOException {
        selector.selectNow();
        for (SelectionKey key : selector.selectedKeys()) {
            selector.selectedKeys().remove(key);
            // Placeholder: handle selected key.
            log.debug("Selected key: {}", key);
        }
    }

    @Override
    public void close() throws IOException {
        serverChannel.close();
        selector.close();
    }
}
