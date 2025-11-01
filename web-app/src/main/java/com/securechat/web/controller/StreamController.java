package com.securechat.web.controller;

import com.securechat.web.model.WebMessage;
import com.securechat.web.service.WebBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
public class StreamController {
    private static final Logger log = LoggerFactory.getLogger(StreamController.class);

    private final WebBridge webBridge;
    private final long keepAliveSeconds;

    public StreamController(WebBridge webBridge,
                            @Value("${app.sse.keepAliveSeconds:15}") long keepAliveSeconds) {
        this.webBridge = webBridge;
        this.keepAliveSeconds = keepAliveSeconds;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("u") String username) {
        String user = username == null ? "" : username.trim();
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username required");
        }
        SseEmitter emitter = webBridge.connect(user, keepAliveSeconds);
        List<WebMessage> history = webBridge.recentMessages();
        for (WebMessage message : history) {
            try {
                emitter.send(SseEmitter.event().name("message").data(message));
            } catch (IOException e) {
                log.warn("Failed to send history message to {}", user, e);
                emitter.completeWithError(e);
                break;
            }
        }
        return emitter;
    }
}
