package com.securechat.webapi.service;

import com.securechat.core.Announcement;
import com.securechat.core.AnnouncementBroadcastHub;
import com.securechat.webapi.store.InMemoryAnnouncementStore;
import com.securechat.webapi.telemetry.NetworkServiceKeys;
import com.securechat.webapi.telemetry.NetworkServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final InMemoryAnnouncementStore announcementStore;
    private final AnnouncementBroadcastHub broadcastHub;
    private final NetworkServiceRegistry networkServiceRegistry;

    @Autowired
    public AnnouncementService(InMemoryAnnouncementStore announcementStore,
                               AnnouncementBroadcastHub broadcastHub,
                               NetworkServiceRegistry networkServiceRegistry) {
        this.announcementStore = announcementStore;
        this.broadcastHub = broadcastHub;
        this.networkServiceRegistry = networkServiceRegistry;
    }

    /**
     * Creates an announcement, stores it in memory, and broadcasts it to the NIO gateway.
     * The in-memory store will handle broadcasting to SSE clients.
     */
    public Announcement createAnnouncement(String author, String title, String content) {
        log.info("   └─ [SERVICE] AnnouncementService.createAnnouncement() - Creating announcement");
        log.info("      → [SERVICE] Calling InMemoryAnnouncementStore.addAnnouncement()");
        
        // Add to the in-memory store, which also handles SSE broadcasting
        Announcement announcement = announcementStore.addAnnouncement(author, title, content);

        log.info("      → [SERVICE] Broadcasting to NIO Gateway (AnnouncementBroadcastHub)");
        // Broadcast to the external NIO gateway
        broadcastHub.broadcast(announcement);
        networkServiceRegistry.recordUsage(
            NetworkServiceKeys.NIO_ANNOUNCEMENTS,
            "BROADCAST",
            String.format("Announcement #%d '%s' forwarded to NIO clients", announcement.getId(), title)
        );

        log.info("📢 ANNOUNCEMENT CREATED: #{} '{}' by {} (broadcast via NIO + SSE)", 
            announcement.getId(), title, author);
        log.info("   └─ [SERVICE] AnnouncementService.createAnnouncement() - Completed");

        return announcement;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementStore.getAllAnnouncements();
    }
}
