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
        emitter.onCompletion(() -> {
            sseEmitters.remove(emitter);
            log.debug("SSE client disconnected (normal closure)");
        });
        emitter.onTimeout(() -> {
            sseEmitters.remove(emitter);
            log.debug("SSE client disconnected (timeout)");
        });
        emitter.onError((ex) -> {
            // This is normal when clients disconnect (close tab, navigate away, etc.)
            // Only log as debug to avoid noise in logs
            String errorMsg = ex.getMessage();
            if (errorMsg != null && (errorMsg.contains("aborted") || errorMsg.contains("reset") || errorMsg.contains("closed"))) {
                log.debug("SSE client disconnected: {}", errorMsg);
            } else {
                log.warn("SSE emitter unexpected error: {}", errorMsg);
            }
            sseEmitters.remove(emitter);
        });
    }

    public void broadcastMessage(MessageEntity message) {
        log.info("   └─ [SERVICE] ChatBroadcastService.broadcastMessage() - Broadcasting via SSE");
        log.info("      → Active SSE emitters: {}", sseEmitters.size());
        log.info("      → Broadcasting to {} connected client(s)", sseEmitters.size());
        
        sseEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
                return false;
            } catch (IOException e) {
                log.debug("      → Removing stale SSE emitter: {}", e.getMessage());
                return true;
            }
        });
        
        log.info("   └─ [SERVICE] ChatBroadcastService.broadcastMessage() - Broadcast complete ({} active emitters)", sseEmitters.size());
    }
}

