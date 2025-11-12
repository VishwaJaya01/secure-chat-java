package com.securechat.webapi.service;

import com.securechat.webapi.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryFileStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryFileStore.class);
    private final ConcurrentHashMap<String, FileMetadata> files = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public List<FileMetadata> getAllFiles() {
        return files.values().stream()
                .sorted((f1, f2) -> f2.createdAt().compareTo(f1.createdAt()))
                .toList();
    }

    public FileMetadata announceFile(String filename, long fileSize, String owner, String host, int port) {
        FileMetadata metadata = FileMetadata.create(filename, fileSize, owner, host, port);
        files.put(metadata.fileId(), metadata);
        log.info("Announced new file: {} ({}) from {}", metadata.filename(), metadata.fileId(), metadata.owner());
        
        // Notify all listening clients
        sendSseEvent("FILE_ANNOUNCED", metadata);
        
        return metadata;
    }

    public void addSseEmitter(SseEmitter emitter) {
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitters.add(emitter);
    }

    private void sendSseEvent(String eventName, Object data) {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to an emitter. Removing it. Error: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
