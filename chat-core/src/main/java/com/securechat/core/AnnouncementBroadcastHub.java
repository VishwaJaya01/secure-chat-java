package com.securechat.core;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Broadcast hub for announcements that allows registering consumers
 * to receive announcement events in real-time.
 */
public class AnnouncementBroadcastHub {
    private final Set<Consumer<Announcement>> consumers = new CopyOnWriteArraySet<>();

    public void register(Consumer<Announcement> consumer) {
        consumers.add(consumer);
    }

    public void unregister(Consumer<Announcement> consumer) {
        consumers.remove(consumer);
    }

    public void broadcast(Announcement announcement) {
        for (Consumer<Announcement> consumer : consumers) {
            try {
                consumer.accept(announcement);
            } catch (Exception e) {
                // Log error but continue broadcasting to other consumers
                System.err.println("Error broadcasting to consumer: " + e.getMessage());
            }
        }
    }

    public int getConsumerCount() {
        return consumers.size();
    }
}



