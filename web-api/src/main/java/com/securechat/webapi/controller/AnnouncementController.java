package com.securechat.webapi.controller;

import com.securechat.core.Announcement;
import com.securechat.webapi.service.AnnouncementService;
import com.securechat.webapi.store.InMemoryAnnouncementStore;
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
        try {
            Announcement announcement = announcementService.createAnnouncement(author, title, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(announcement);
        } catch (Exception e) {
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

