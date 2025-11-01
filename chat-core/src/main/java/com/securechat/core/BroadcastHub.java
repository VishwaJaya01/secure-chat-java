package com.securechat.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Placeholder hub to fan-out messages to registered listeners.
 */
public class BroadcastHub {
    private final Set<String> listeners = Collections.synchronizedSet(new LinkedHashSet<>());

    public void register(String id) {
        listeners.add(id);
    }

    public void unregister(String id) {
        listeners.remove(id);
    }

    public Set<String> snapshotListeners() {
        synchronized (listeners) {
            return Set.copyOf(listeners);
        }
    }

    public void broadcast(Message message) {
        // Placeholder no-op implementation for now.
    }
}
