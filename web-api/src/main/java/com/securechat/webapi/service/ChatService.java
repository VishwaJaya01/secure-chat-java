package com.securechat.webapi.service;

import com.securechat.core.Message;
import com.securechat.webapi.entity.MessageEntity;
import com.securechat.webapi.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public MessageEntity sendMessage(String username, String text) {
        MessageEntity message = new MessageEntity();
        message.setAuthor(username);
        message.setContent(text);
        return messageRepository.save(message);
    }

    public List<MessageEntity> getRecentMessages() {
        return messageRepository.findTop200ByOrderByCreatedAtDesc();
    }
}

