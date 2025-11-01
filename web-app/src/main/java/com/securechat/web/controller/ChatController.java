package com.securechat.web.controller;

import com.securechat.web.service.MessageLogService;
import com.securechat.web.service.WebBridge;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ChatController {
    private final MessageLogService messageLogService;
    private final WebBridge webBridge;

    public ChatController(MessageLogService messageLogService, WebBridge webBridge) {
        this.messageLogService = messageLogService;
        this.webBridge = webBridge;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("messages", messageLogService.findRecent());
        return "index";
    }

    @PostMapping("/send")
    public String send(@RequestParam String user, @RequestParam String content) {
        webBridge.forward(user, content);
        return "redirect:/";
    }
}
