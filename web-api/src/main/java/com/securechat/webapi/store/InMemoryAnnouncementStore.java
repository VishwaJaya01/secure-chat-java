package com.securechat.webapi.store;

import com.securechat.core.Announcement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An in-memory, thread-safe store for announcements.
 * Manages the list of announcements and broadcasts new ones to SSE clients.
 */
@Component
public class InMemoryAnnouncementStore {
    private static final Logger log = LoggerFactory.getLogger(InMemoryAnnouncementStore.class);

    private final AtomicLong sequence = new AtomicLong(0);
    private final List<Announcement> announcements = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    /**
     * Adds a new announcement to the store and broadcasts it to all listeners.
     */
    public Announcement addAnnouncement(String author, String title, String content) {
        log.info("      └─ [STORE] InMemoryAnnouncementStore.addAnnouncement() - Storing in memory");
        
        Announcement announcement = new Announcement(
                sequence.incrementAndGet(),
                author,
                title,
                content,
                Instant.now()
        );
        announcements.add(0, announcement); // Add to the front of the list
        log.info("      → Announcement stored with ID: #{}", announcement.getId());
        
        log.info("      → [SERVICE] Broadcasting to SSE clients");
        broadcast(announcement);
        
        log.info("      └─ [STORE] InMemoryAnnouncementStore.addAnnouncement() - Completed");
        return announcement;
    }

    /**
     * Returns a read-only list of all announcements, sorted by most recent first.
     */
    public List<Announcement> getAllAnnouncements() {
        return List.copyOf(announcements);
    }

    /**
     * Registers a new SSE emitter to receive future announcements.
     */
    public void addSseEmitter(SseEmitter emitter) {
        // Send existing announcements to the new client
        try {
            for (Announcement announcement : announcements) {
                emitter.send(SseEmitter.event().name("announcement").data(announcement));
            }
        } catch (IOException e) {
            log.warn("Failed to send initial announcements to new SSE client: {}", e.getMessage());
            emitter.completeWithError(e);
        }

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError((ex) -> sseEmitters.remove(emitter));

        sseEmitters.add(emitter);
        log.info("New SSE emitter registered. Total emitters: {}", sseEmitters.size());
    }

    /**
     * Broadcasts an announcement to all registered SSE emitters.
     */
    private void broadcast(Announcement announcement) {
        log.info("         → Active SSE emitters: {}", sseEmitters.size());
        sseEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("announcement").data(announcement));
                return false; // Keep emitter
            } catch (Exception e) {
                log.debug("         → Removing stale SSE emitter: {}", e.getMessage());
                return true; // Remove emitter
            }
        });
        log.info("         → Broadcasted announcement #{} to {} SSE client(s)", announcement.getId(), sseEmitters.size());
    }
}
