package com.securechat.webapi.controller;

import com.securechat.webapi.entity.LinkPreviewEntity;
import com.securechat.webapi.service.LinkPreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/link")
@CrossOrigin(origins = "http://localhost:5173")
public class LinkPreviewController {
    private static final Logger log = LoggerFactory.getLogger(LinkPreviewController.class);
    private final LinkPreviewService linkPreviewService;

    @Autowired
    public LinkPreviewController(LinkPreviewService linkPreviewService) {
        this.linkPreviewService = linkPreviewService;
    }

    @GetMapping("/preview")
    public ResponseEntity<LinkPreviewEntity> getPreview(@RequestParam String url) {
        try {
            LinkPreviewEntity preview = linkPreviewService.getOrFetchPreview(url);
            return ResponseEntity.ok(preview);
        } catch (SecurityException e) {
            log.warn("🔗 LINK PREVIEW (API REQUEST BLOCKED): {} - SSRF protection", url);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            log.warn("🔗 LINK PREVIEW (API REQUEST INVALID): {} - {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("🔗 LINK PREVIEW (API REQUEST ERROR): {} - {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}



