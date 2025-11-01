package com.securechat.nio;

import com.securechat.core.Message;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Placeholder session state for a single NIO channel.
 */
public class ChannelSession {
    private final SocketChannel channel;

    public ChannelSession(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void send(Message message) {
        // Placeholder no-op. Real implementation would serialize the message.
    }

    public ByteBuffer drainBuffer() {
        return ByteBuffer.allocate(0);
    }
}
