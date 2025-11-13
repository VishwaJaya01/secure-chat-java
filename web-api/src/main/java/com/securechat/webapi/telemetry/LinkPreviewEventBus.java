package com.securechat.webapi.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

@Component
public class LinkPreviewEventBus {
    private static final Logger log = LoggerFactory.getLogger(LinkPreviewEventBus.class);
    private static final int MAX_HISTORY = 25;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final LinkedBlockingDeque<LinkPreviewEvent> history = new LinkedBlockingDeque<>(MAX_HISTORY);

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        // Send recent events to new subscriber
        history.descendingIterator().forEachRemaining(event -> sendEvent(emitter, event));
        return emitter;
    }

    public void publish(LinkPreviewEvent event) {
        if (history.remainingCapacity() == 0) {
            history.pollFirst();
        }
        history.offerLast(event);
        emitters.removeIf(emitter -> !sendEvent(emitter, event));
    }

    private boolean sendEvent(SseEmitter emitter, LinkPreviewEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("link-preview")
                    .data(event));
            return true;
        } catch (IOException e) {
            log.debug("Removing stale link preview SSE emitter: {}", e.getMessage());
            return false;
        }
    }
}
