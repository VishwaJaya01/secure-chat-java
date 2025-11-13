package com.securechat.webapi.controller;

import com.securechat.webapi.entity.UserEntity;
import com.securechat.webapi.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PresenceController {
    private static final Logger log = LoggerFactory.getLogger(PresenceController.class);
    private final PresenceService presenceService;
    private final int httpLogEvery;
    private final AtomicLong logCounter = new AtomicLong(0);

    @Autowired
    public PresenceController(PresenceService presenceService,
                              @Value("${presence.http.log-every:40}") int httpLogEvery) {
        this.presenceService = presenceService;
        this.httpLogEvery = Math.max(1, httpLogEvery);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserEntity>> getUsers() {
        List<UserEntity> users = presenceService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/status")
    public ResponseEntity<Map<String, String>> getUserStatus() {
        Map<String, String> status = presenceService.getUserPresenceStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/presence/beat")
    public ResponseEntity<Void> updatePresence(
            @RequestParam String userId,
            @RequestParam(required = false) String displayName) {
        boolean logRequest = shouldLogThisHeartbeat();
        if (logRequest) {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📥 [CONTROLLER] PresenceController.updatePresence() - HTTP POST /api/presence/beat");
            log.info("   → Service Flow: PresenceController → PresenceService");
            log.info("   → Parameters: userId={}, displayName={}", userId, displayName);
            log.info("   → [SERVICE] Calling PresenceService.updatePresence()");
        } else {
            log.debug("📥 Presence heartbeat userId={} displayName={}", userId, displayName);
        }
        
        presenceService.updatePresence(userId, displayName);
        
        if (logRequest) {
            log.info("✅ [CONTROLLER] PresenceController.updatePresence() - Request completed");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
        return ResponseEntity.noContent().build();
    }

    private boolean shouldLogThisHeartbeat() {
        long count = logCounter.incrementAndGet();
        return httpLogEvery <= 1 || count % httpLogEvery == 1;
    }
}




