package com.securechat.webapi.store;

import com.securechat.core.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FileMetaStore {

    private static final Logger log = LoggerFactory.getLogger(FileMetaStore.class);

    private final ConcurrentHashMap<String, FileMetadata> files = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public FileMetadata save(FileMetadata fileMetadata) {
        Objects.requireNonNull(fileMetadata, "fileMetadata must not be null");
        files.put(fileMetadata.fileId(), fileMetadata);
        broadcast("file-announced", fileMetadata);
        return fileMetadata;
    }

    public List<FileMetadata> findAll() {
        List<FileMetadata> fileList = new ArrayList<>(files.values());
        fileList.sort((f1, f2) -> f2.createdAt().compareTo(f1.createdAt()));
        return Collections.unmodifiableList(fileList);
    }

    public FileMetadata findById(String fileId) {
        return files.get(fileId);
    }

    public FileMetadata findByFilename(String filename) {
        return files.values().stream()
            .filter(f -> f.filename().equals(filename))
            .findFirst()
            .orElse(null);
    }

    public void remove(String fileId) {
        FileMetadata removed = files.remove(fileId);
        if (removed != null) {
            broadcast("file-removed", removed);
        }
    }

    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> {
            log.warn("SSE emitter error: {}", e.getMessage());
            emitters.remove(emitter);
        });
        return emitter;
    }

    private void broadcast(String eventName, Object payload) {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
            .name(eventName)
            .data(payload);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException ex) {
                log.debug("Removing stale SSE emitter due to send failure: {}", ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
