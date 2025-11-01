package com.securechat.web.repository;

import com.securechat.core.Message;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class MessageRepository {
    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());

    public void save(Message message) {
        messages.add(message);
    }

    public List<Message> findRecent() {
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }
}
