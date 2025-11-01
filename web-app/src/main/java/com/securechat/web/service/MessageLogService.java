package com.securechat.web.service;

import com.securechat.core.Message;
import com.securechat.web.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class MessageLogService {
    private static final Logger log = LoggerFactory.getLogger(MessageLogService.class);
    private final MessageRepository messageRepository;

    public MessageLogService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void append(Message message) {
        messageRepository.save(message);
    }

    public List<Message> findRecent() {
        return messageRepository.findRecent();
    }

    public void streamRecent(SseEmitter emitter) {
        try {
            for (Message message : findRecent()) {
                emitter.send(message.getFrom() + ": " + message.getContent());
            }
            emitter.complete();
        } catch (IOException e) {
            log.warn("SSE stream failed", e);
            emitter.completeWithError(e);
        }
    }
}
