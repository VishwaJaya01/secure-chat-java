package com.securechat.webapi.controller;

import com.securechat.core.Announcement;
import com.securechat.webapi.service.AnnouncementService;
import com.securechat.webapi.store.InMemoryAnnouncementStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "http://localhost:5173")
public class AnnouncementController {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementController.class);
    private final AnnouncementService announcementService;
    private final InMemoryAnnouncementStore announcementStore;

    @Autowired
    public AnnouncementController(AnnouncementService announcementService, InMemoryAnnouncementStore announcementStore) {
        this.announcementService = announcementService;
        this.announcementStore = announcementStore;
    }

    @PostMapping
    public ResponseEntity<Announcement> createAnnouncement(
            @RequestParam String author,
            @RequestParam String title,
            @RequestParam String content) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📥 [CONTROLLER] AnnouncementController.createAnnouncement() - HTTP POST /api/announcements");
        log.info("   → Service Flow: AnnouncementController → AnnouncementService → InMemoryAnnouncementStore → NIO Gateway");
        log.info("   → Parameters: author={}, title={}, content={}", author, title, 
            content.length() > 50 ? content.substring(0, 50) + "..." : content);
        log.info("   → [SERVICE] Calling AnnouncementService.createAnnouncement()");
        
        try {
            Announcement announcement = announcementService.createAnnouncement(author, title, content);
            log.info("✅ [CONTROLLER] AnnouncementController.createAnnouncement() - Request completed");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return ResponseEntity.status(HttpStatus.CREATED).body(announcement);
        } catch (Exception e) {
            log.error("❌ [CONTROLLER] AnnouncementController.createAnnouncement() - Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Announcement>> getAllAnnouncements() {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        return ResponseEntity.ok(announcements);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnnouncements() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        announcementStore.addSseEmitter(emitter);
        return emitter;
    }
}




