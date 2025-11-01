package com.securechat.web.controller;

import com.securechat.web.model.WebMessage;
import com.securechat.web.service.WebBridge;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ChatController {
    private final WebBridge webBridge;

    public ChatController(WebBridge webBridge) {
        this.webBridge = webBridge;
    }

    @GetMapping("/")
    public String login(@RequestParam(value = "username", required = false) String username,
                        @RequestParam(value = "error", required = false) String error,
                        Model model) {
        model.addAttribute("username", username == null ? "" : username);
        model.addAttribute("error", error);
        model.addAttribute("pageTitle", "SecureChat");
        return "login";
    }

    @PostMapping("/join")
    public String join(@RequestParam("username") String username,
                       Model model) {
        String sanitized = username == null ? "" : username.trim();
        if (sanitized.isEmpty()) {
            model.addAttribute("error", "Please choose a username");
            model.addAttribute("username", "");
            model.addAttribute("pageTitle", "SecureChat");
            return "login";
        }
        webBridge.registerUser(sanitized);
        return "redirect:/chat?u=" + sanitized;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "u", required = false) String username,
                       Model model) {
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Please choose a username");
            model.addAttribute("username", "");
            model.addAttribute("pageTitle", "SecureChat");
            return "login";
        }
        String sanitized = username.trim();
        webBridge.registerUser(sanitized);
        List<WebMessage> messages = webBridge.recentMessages().stream()
                .map(m -> m.withMine(sanitized.equalsIgnoreCase(m.user())))
                .toList();
        model.addAttribute("username", sanitized);
        model.addAttribute("pageTitle", "SecureChat");
        model.addAttribute("initialMessages", messages);
        model.addAttribute("onlineUsers", webBridge.onlineUsers());
        return "chat";
    }
}
