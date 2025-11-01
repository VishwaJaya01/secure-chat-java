package com.securechat.web.service;

import com.securechat.core.BroadcastHub;
import com.securechat.core.Message;
import com.securechat.core.UserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WebBridge {
    private static final Logger log = LoggerFactory.getLogger(WebBridge.class);
    private final BroadcastHub hub;
    private final UserRegistry userRegistry;
    private final MessageLogService messageLogService;

    public WebBridge(BroadcastHub hub, UserRegistry userRegistry, MessageLogService messageLogService) {
        this.hub = hub;
        this.userRegistry = userRegistry;
        this.messageLogService = messageLogService;
    }

    public void forward(String user, String content) {
        Message message = new Message(user, content, Instant.now());
        userRegistry.register(user, user);
        messageLogService.append(message);
        hub.broadcast(message);
        log.info("Forwarded message from '{}'", user);
    }
}
