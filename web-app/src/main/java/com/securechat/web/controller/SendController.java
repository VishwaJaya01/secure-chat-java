package com.securechat.web.controller;

import com.securechat.web.model.WebMessage;
import com.securechat.web.service.WebBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SendController {
    private static final Logger log = LoggerFactory.getLogger(SendController.class);

    private final WebBridge webBridge;

    public SendController(WebBridge webBridge) {
        this.webBridge = webBridge;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> send(@RequestParam("username") String username,
                                     @RequestParam("text") String text) {
        String user = username == null ? "" : username.trim();
        String body = text == null ? "" : text.trim();
        if (user.isEmpty() || body.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("User '{}' submitting message", user);
        webBridge.broadcast(WebMessage.chat(user, body));
        return ResponseEntity.noContent().build();
    }
}
