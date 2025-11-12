package com.securechat.webapi.service;

import com.securechat.webapi.entity.MessageEntity;
import com.securechat.webapi.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(?i)\\b(https?://[\\w.-]+(?:\\.[\\w.-]+)*(?:/[^\\s]*)?|www\\.[\\w.-]+(?:\\.[\\w.-]+)*(?:/[^\\s]*)?)"
    );

    private final MessageRepository messageRepository;
    private final LinkPreviewService linkPreviewService;

    @Autowired
    public ChatService(MessageRepository messageRepository, LinkPreviewService linkPreviewService) {
        this.messageRepository = messageRepository;
        this.linkPreviewService = linkPreviewService;
    }

    @Transactional
    public MessageEntity sendMessage(String username, String text) {
        log.info("   └─ [SERVICE] ChatService.sendMessage() - Persisting message to database");
        log.info("      → Using MessageRepository (JPA/Hibernate)");
        
        MessageEntity message = new MessageEntity();
        message.setAuthor(username);
        message.setContent(text);
        MessageEntity saved = messageRepository.save(message);
        log.info("      → Message saved with ID: {}", saved.getId());
        
        // Asynchronously fetch link previews for URLs in the message
        List<String> urls = extractUrls(text);
        if (!urls.isEmpty()) {
            log.info("      → [SERVICE] Detected {} URL(s), calling LinkPreviewService", urls.size());
            urls.forEach(url -> {
                try {
                    linkPreviewService.getOrFetchPreview(url);
                } catch (Exception e) {
                    log.debug("      → Link preview fetch failed for {}: {}", url, e.getMessage());
                }
            });
        }
        
        log.info("   └─ [SERVICE] ChatService.sendMessage() - Completed");
        return saved;
    }

    public List<MessageEntity> getRecentMessages() {
        return messageRepository.findTop200ByOrderByCreatedAtDesc();
    }

    /**
     * Extract URLs from text content.
     */
    public List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            // Normalize URLs
            if (url.startsWith("www.")) {
                url = "https://" + url;
            }
            try {
                // Validate URL
                new URL(url);
                urls.add(url);
            } catch (Exception e) {
                // Skip invalid URLs
            }
        }
        return urls;
    }
}



