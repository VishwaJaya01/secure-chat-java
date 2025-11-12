package com.securechat.webapi.controller;

import com.securechat.webapi.entity.TaskEntity;
import com.securechat.webapi.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskEntity> createTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String createdBy,
            @RequestParam(required = false) String assignee) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📥 [CONTROLLER] TaskController.createTask() - HTTP POST /api/tasks");
        log.info("   → Service Flow: TaskController → TaskService → SSE Broadcast");
        log.info("   → Parameters: title={}, createdBy={}, assignee={}", title, createdBy, assignee);
        log.info("   → [SERVICE] Calling TaskService.createTask()");
        
        TaskEntity task = taskService.createTask(title, description, createdBy, assignee);
        log.info("📋 TASK CREATED: #{} '{}' by {} (Status: {}, Assignee: {})", 
            task.getId(), task.getTitle(), task.getCreatedBy(), task.getStatus(), 
            task.getAssignee() != null ? task.getAssignee() : "Unassigned");
        
        log.info("   → [SERVICE] Broadcasting task update via SSE");
        broadcastToSseClients(task);
        
        log.info("✅ [CONTROLLER] TaskController.createTask() - Request completed");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @GetMapping
    public ResponseEntity<List<TaskEntity>> getAllTasks() {
        List<TaskEntity> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskEntity> getTask(@PathVariable Long id) {
        try {
            TaskEntity task = taskService.getTaskById(id);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskEntity> updateTask(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignee) {
        try {
            TaskEntity oldTask = taskService.getTaskById(id);
            TaskEntity task = taskService.updateTask(id, title, description, status, assignee);
            
            // Log status changes prominently
            if (status != null && !status.equals(oldTask.getStatus())) {
                log.info("🔄 TASK MOVED: #{} '{}' from {} → {} (Assignee: {})", 
                    task.getId(), task.getTitle(), oldTask.getStatus(), task.getStatus(),
                    task.getAssignee() != null ? task.getAssignee() : "Unassigned");
            } else {
                log.info("✏️  TASK UPDATED: #{} '{}' (Status: {}, Assignee: {})", 
                    task.getId(), task.getTitle(), task.getStatus(),
                    task.getAssignee() != null ? task.getAssignee() : "Unassigned");
            }
            
            broadcastToSseClients(task);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        try {
            TaskEntity task = taskService.getTaskById(id);
            taskService.deleteTask(id);
            log.info("🗑️  TASK DELETED: #{} '{}'", id, task.getTitle());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTasks() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError((ex) -> sseEmitters.remove(emitter));

        // Send existing tasks
        try {
            List<TaskEntity> tasks = taskService.getAllTasks();
            for (TaskEntity task : tasks) {
                emitter.send(SseEmitter.event()
                        .name("task")
                        .data(task));
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void broadcastToSseClients(TaskEntity task) {
        sseEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("task")
                        .data(task));
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }
}




