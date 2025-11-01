package com.securechat.web.service;

import com.securechat.web.model.UserView;
import com.securechat.web.model.WebMessage;
import com.securechat.web.repository.MessageRepository;
import com.securechat.web.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Profile("db")
public class MessageLogService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageLogService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public void save(WebMessage message) {
        messageRepository.save(message);
        userRepository.upsert(message.user());
    }

    public List<WebMessage> findRecent(int limit) {
        List<WebMessage> messages = messageRepository.findRecent(limit);
        Collections.reverse(messages);
        return messages;
    }

    public List<UserView> findUsers() {
        return userRepository.findAll();
    }

    public void registerUser(String username) {
        userRepository.upsert(username);
    }
}
