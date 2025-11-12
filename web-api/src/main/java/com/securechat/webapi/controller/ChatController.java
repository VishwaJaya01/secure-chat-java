package com.securechat.webapi.controller;

import com.securechat.webapi.entity.MessageEntity;
import com.securechat.webapi.service.ChatBroadcastService;
import com.securechat.webapi.service.ChatService;
import com.securechat.webapi.service.InternalTlsChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final ChatBroadcastService chatBroadcastService;
    private final InternalTlsChatClient internalTlsChatClient;

    @Autowired
    public ChatController(ChatService chatService, ChatBroadcastService chatBroadcastService, 
                         InternalTlsChatClient internalTlsChatClient) {
        this.chatService = chatService;
        this.chatBroadcastService = chatBroadcastService;
        this.internalTlsChatClient = internalTlsChatClient;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(
            @RequestParam String username,
            @RequestParam String text) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📥 [CONTROLLER] ChatController.sendMessage() - HTTP POST /api/send");
        log.info("   → Service Flow: ChatController → InternalTlsChatClient → ChatService → ChatBroadcastService");
        log.info("   → Parameters: username={}, text={}", username, text.length() > 50 ? text.substring(0, 50) + "..." : text);
        
        // Route message through TLS to demonstrate TLS encryption
        log.info("   → [SERVICE] Calling InternalTlsChatClient.sendMessageViaTls()");
        internalTlsChatClient.sendMessageViaTls(username, text);
        
        // Also persist and broadcast via normal flow
        log.info("   → [SERVICE] Calling ChatService.sendMessage()");
        MessageEntity message = chatService.sendMessage(username, text);
        String preview = text.length() > 50 ? text.substring(0, 50) + "..." : text;
        log.info("💬 CHAT MESSAGE: {} → {} (routed through TLS)", username, preview);
        
        log.info("   → [SERVICE] Calling ChatBroadcastService.broadcastMessage()");
        chatBroadcastService.broadcastMessage(message);
        
        log.info("✅ [CONTROLLER] ChatController.sendMessage() - Request completed");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessages(@RequestParam(required = false) String u) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        chatBroadcastService.registerEmitter(emitter);

        // Send recent messages
        try {
            List<MessageEntity> messages = chatService.getRecentMessages();
            for (MessageEntity message : messages) {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}



