package com.securechat.webapi.controller;

import com.securechat.webapi.entity.UserEntity;
import com.securechat.webapi.service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PresenceController {
    private final PresenceService presenceService;

    @Autowired
    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
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
        presenceService.updatePresence(userId, displayName);
        return ResponseEntity.noContent().build();
    }
}

