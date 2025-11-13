package com.securechat.webapi.service;

import com.securechat.core.Message;
import com.securechat.webapi.entity.MessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TlsChatService {
    private static final Logger log = LoggerFactory.getLogger(TlsChatService.class);
    
    private final ChatService chatService;
    private final ChatBroadcastService chatBroadcastService;

    public TlsChatService(ChatService chatService, ChatBroadcastService chatBroadcastService) {
        this.chatService = chatService;
        this.chatBroadcastService = chatBroadcastService;
    }

    public void handleTlsMessage(Message message) {
        String preview = message.getContent().length() > 50 ? message.getContent().substring(0, 50) + "..." : message.getContent();
        log.info("💬 CHAT MESSAGE (TLS): {} → {}", message.getFrom(), preview);
        
        // Persist message via ChatService
        MessageEntity entity = chatService.sendMessage(message.getFrom(), message.getContent());
        
        // Broadcast to SSE clients (HTTP chat clients)
        chatBroadcastService.broadcastMessage(entity);
    }
}

