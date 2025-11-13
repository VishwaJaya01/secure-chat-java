package com.securechat.announce.nio;

import com.securechat.core.Announcement;
import com.securechat.core.AnnouncementBroadcastHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * NIO-based gateway for broadcasting announcements to multiple connected clients.
 * Uses a single-threaded selector to handle many clients without blocking.
 */
public class AnnouncementGateway implements Runnable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementGateway.class);
    private static final int BUFFER_SIZE = 8192;
    private static final int DEFAULT_PORT = 6001;

    private final int port;
    private final AnnouncementBroadcastHub broadcastHub;
    private final Consumer<String> messageConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile int currentPort = DEFAULT_PORT;
    private Thread selectorThread;
    private final Set<SocketChannel> connectedChannels = ConcurrentHashMap.newKeySet();

    public AnnouncementGateway(int port, AnnouncementBroadcastHub broadcastHub, Consumer<String> messageConsumer) {
        this.port = port;
        this.broadcastHub = broadcastHub;
        this.messageConsumer = messageConsumer;
    }

    /**
     * Initialize the server socket channel and selector.
     */
    public void start() throws IOException {
        if (running.get()) {
            log.warn("Gateway is already running");
            return;
        }

        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        currentPort = port;
        running.set(true);
        selectorThread = new Thread(this, "AnnouncementGateway-Selector");
        selectorThread.setDaemon(true);
        selectorThread.start();

        log.info("AnnouncementGateway started on port {}", currentPort);
    }

    @Override
    public void run() {
        log.info("Selector thread started");

        while (running.get()) {
            try {
                selector.select(); // Block until an event occurs

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (Exception e) {
                        log.error("Error handling key: {}", key, e);
                        closeChannel(key);
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error in selector loop", e);
                }
            }
        }

        log.info("Selector thread stopped");
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new ClientSession(clientChannel));

        connectedChannels.add(clientChannel);
        log.info("Client connected: {} (total: {})", clientChannel.getRemoteAddress(), connectedChannels.size());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = channel.read(buffer);

        if (bytesRead == -1) {
            closeChannel(key);
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            String message = StandardCharsets.UTF_8.decode(buffer).toString().trim();
            log.debug("Received from {}: {}", channel.getRemoteAddress(), message);

            // Pass the raw message to the consumer for processing
            if (messageConsumer != null) {
                messageConsumer.accept(message);
            }
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = (ClientSession) key.attachment();

        ByteBuffer buffer = session.getWriteBuffer();
        if (buffer != null && buffer.hasRemaining()) {
            channel.write(buffer);
            if (!buffer.hasRemaining()) {
                key.interestOps(SelectionKey.OP_READ);
                session.clearWriteBuffer();
            }
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public void broadcast(Announcement announcement) {
        if (!running.get()) {
            log.warn("Cannot broadcast: gateway not running");
            return;
        }

        String message = formatAnnouncement(announcement);
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        int sentCount = 0;
        int connectedCount = connectedChannels.size();
        log.info("📣 Broadcast queued for announcement '{}' by {} ({} connected clients)",
                announcement.getTitle(), announcement.getAuthor(), connectedCount);

        for (SocketChannel channel : connectedChannels) {
            if (channel.isOpen() && channel.isConnected()) {
                try {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null && key.isValid()) {
                        ClientSession session = (ClientSession) key.attachment();
                        if (session != null) {
                            ByteBuffer writeBuffer = ByteBuffer.allocate(buffer.capacity());
                            buffer.rewind();
                            writeBuffer.put(buffer);
                            writeBuffer.flip();
                            session.setWriteBuffer(writeBuffer);
                            key.interestOps(SelectionKey.OP_WRITE);
                            sentCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error preparing broadcast to {}", safeRemoteAddress(channel), e);
                }
            }
        }

        if (sentCount > 0) {
            selector.wakeup();
            log.info("Broadcasted announcement to {} clients", sentCount);
        } else {
            log.info("No active NIO clients when '{}' was broadcast – announcement still delivered via REST/SSE",
                    announcement.getTitle());
        }
    }

    private String formatAnnouncement(Announcement announcement) {
        return String.format("ANNOUNCE|%d|%s|%s|%s|%s\n",
                announcement.getId() != null ? announcement.getId() : 0,
                announcement.getAuthor(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getCreatedAt().toString());
    }

    private void closeChannel(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            connectedChannels.remove(channel);
            log.info("Client disconnected: {} (total: {})",
                    channel.getRemoteAddress(), connectedChannels.size());
            key.cancel();
            channel.close();
        } catch (IOException e) {
            log.error("Error closing channel", e);
        }
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        if (selector != null) {
            selector.wakeup();
        }
        if (selectorThread != null) {
            try {
                selectorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (serverChannel != null) serverChannel.close();
        if (selector != null) selector.close();
        log.info("AnnouncementGateway closed");
    }

    private static class ClientSession {
        private final SocketChannel channel;
        private ByteBuffer writeBuffer;

        ClientSession(SocketChannel channel) {
            this.channel = channel;
        }

        ByteBuffer getWriteBuffer() {
            return writeBuffer;
        }

        void setWriteBuffer(ByteBuffer writeBuffer) {
            this.writeBuffer = writeBuffer;
        }

        void clearWriteBuffer() {
            this.writeBuffer = null;
        }
    }

    private static String safeRemoteAddress(SocketChannel channel) {
        try {
            return String.valueOf(channel.getRemoteAddress());
        } catch (IOException e) {
            return "unknown";
        }
    }
}
