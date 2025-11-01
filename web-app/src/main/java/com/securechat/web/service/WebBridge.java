package com.securechat.web.service;

import com.securechat.core.BroadcastHub;
import com.securechat.core.Message;
import com.securechat.web.model.UserView;
import com.securechat.web.model.WebMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class WebBridge {
    private static final Logger log = LoggerFactory.getLogger(WebBridge.class);
    private static final int RECENT_LIMIT = 50;

    private final BroadcastHub hub;
    private final MessageLogService messageLogService;
    private final PresenceService presenceService;
    private final ScheduledExecutorService scheduler;
    private final Deque<WebMessage> recent = new ArrayDeque<>();
    private final CopyOnWriteArrayList<SseSession> sessions = new CopyOnWriteArrayList<>();

    public WebBridge(BroadcastHub hub,
                     ObjectProvider<MessageLogService> messageLogServiceProvider,
                     PresenceService presenceService,
                     ScheduledExecutorService scheduler) {
        this.hub = hub;
        this.messageLogService = messageLogServiceProvider.getIfAvailable();
        this.presenceService = presenceService;
        this.scheduler = scheduler;
    }

    public void registerUser(String username) {
        presenceService.markOnline(username);
        if (messageLogService != null) {
            messageLogService.registerUser(username);
        }
    }

    public List<UserView> onlineUsers() {
        if (messageLogService != null) {
            return messageLogService.findUsers();
        }
        return presenceService.snapshot();
    }

    public void broadcast(WebMessage message) {
        WebMessage enriched = message.withTimestamp(Instant.now());
        log.info("Broadcasting message from {}", enriched.user());
        hub.broadcast(new Message(enriched.user(), enriched.text(), enriched.timestamp()));
        addRecent(enriched);
        if (messageLogService != null) {
            messageLogService.save(enriched);
        }
        emit(enriched);
    }

    public List<WebMessage> recentMessages() {
        if (messageLogService != null) {
            return messageLogService.findRecent(RECENT_LIMIT);
        }
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    public SseEmitter connect(String username, long keepAliveSeconds) {
        SseEmitter emitter = new SseEmitter(0L);
        SseSession session = new SseSession(username, emitter);
        sessions.add(session);
        try {
            emitter.send(SseEmitter.event().name("connected").data(username, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event for user '{}': {}", username, e.getMessage());
        }
        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(
                () -> sendKeepAlive(session),
                keepAliveSeconds,
                keepAliveSeconds,
                TimeUnit.SECONDS);
        session.setHeartbeat(heartbeat);
        emitter.onCompletion(() -> cleanup(session));
        emitter.onTimeout(() -> cleanup(session));
        emitter.onError(e -> cleanup(session));
        presenceService.markOnline(username);
        log.info("SSE client connected: {}", username);
        return emitter;
    }

    private void sendKeepAlive(SseSession session) {
        try {
            session.emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException e) {
            cleanup(session);
        }
    }

    private void cleanup(SseSession session) {
        sessions.remove(session);
        if (session.heartbeat != null) {
            session.heartbeat.cancel(true);
        }
        presenceService.markOffline(session.username);
        log.info("SSE client disconnected: {}", session.username);
    }

    private void emit(WebMessage message) {
        for (SseSession session : sessions) {
            try {
                boolean mine = message.user().equalsIgnoreCase(session.username);
                WebMessage payload = message.withMine(mine);
                session.emitter.send(SseEmitter.event()
                        .name("message")
                        .data(payload));
            } catch (IOException e) {
                cleanup(session);
            }
        }
    }

    private void addRecent(WebMessage message) {
        synchronized (recent) {
            if (recent.size() == RECENT_LIMIT) {
                recent.removeFirst();
            }
            recent.addLast(message);
        }
    }

    private static final class SseSession {
        private final String username;
        private final SseEmitter emitter;
        private ScheduledFuture<?> heartbeat;

        private SseSession(String username, SseEmitter emitter) {
            this.username = username;
            this.emitter = emitter;
        }

        private void setHeartbeat(ScheduledFuture<?> heartbeat) {
            this.heartbeat = heartbeat;
        }
    }
}
