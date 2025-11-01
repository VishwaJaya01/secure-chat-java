package com.securechat.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placeholder registry mapping user ids to display metadata.
 */
public class UserRegistry {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    public void register(String userId, String displayName) {
        users.put(userId, displayName);
    }

    public void unregister(String userId) {
        users.remove(userId);
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(users);
    }
}
