package com.securechat.web.service;

import com.securechat.web.model.UserView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PresenceService {
    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final Map<String, AtomicInteger> onlineUsers = new ConcurrentHashMap<>();

    public void markOnline(String username) {
        onlineUsers.compute(username, (key, counter) -> {
            if (counter == null) {
                log.info("User '{}' marked online", username);
                return new AtomicInteger(1);
            }
            counter.incrementAndGet();
            return counter;
        });
    }

    public void markOffline(String username) {
        onlineUsers.computeIfPresent(username, (key, counter) -> {
            int remaining = counter.decrementAndGet();
            if (remaining <= 0) {
                log.info("User '{}' marked offline", username);
                return null;
            }
            return counter;
        });
    }

    public List<UserView> snapshot() {
        return onlineUsers.keySet().stream()
                .sorted()
                .map(user -> new UserView(user, "online"))
                .collect(Collectors.toList());
    }
}
