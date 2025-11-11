package com.securechat.webapi.controller;

import com.securechat.webapi.entity.MessageEntity;
import com.securechat.webapi.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    private final ChatService chatService;
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(
            @RequestParam String username,
            @RequestParam String text) {
        MessageEntity message = chatService.sendMessage(username, text);
        broadcastToSseClients(message);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessages(@RequestParam(required = false) String u) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError((ex) -> sseEmitters.remove(emitter));

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

    private void broadcastToSseClients(MessageEntity message) {
        sseEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }
}

