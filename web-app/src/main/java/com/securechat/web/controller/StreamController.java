package com.securechat.web.controller;

import com.securechat.web.service.MessageLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamController {
    private final MessageLogService messageLogService;

    public StreamController(MessageLogService messageLogService) {
        this.messageLogService = messageLogService;
    }

    @GetMapping(path = "/stream")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter();
        messageLogService.streamRecent(emitter);
        return emitter;
    }
}
