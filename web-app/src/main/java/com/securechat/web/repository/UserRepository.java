package com.securechat.web.repository;

import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Repository
public class UserRepository {
    private final Set<String> users = Collections.synchronizedSet(new LinkedHashSet<>());

    public void register(String userId) {
        users.add(userId);
    }

    public Set<String> findAll() {
        synchronized (users) {
            return Set.copyOf(users);
        }
    }
}
