package com.securechat.webapi.service;

import com.securechat.webapi.entity.MessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ChatBroadcastService {
    private static final Logger log = LoggerFactory.getLogger(ChatBroadcastService.class);
    
    private final CopyOnWriteArrayList<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public void registerEmitter(SseEmitter emitter) {
        sseEmitters.add(emitter);
        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError((ex) -> {
            log.warn("SSE emitter error: {}", ex.getMessage());
            sseEmitters.remove(emitter);
        });
    }

    public void broadcastMessage(MessageEntity message) {
        sseEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
                return false;
            } catch (IOException e) {
                log.debug("Removing stale SSE emitter: {}", e.getMessage());
                return true;
            }
        });
    }
}

