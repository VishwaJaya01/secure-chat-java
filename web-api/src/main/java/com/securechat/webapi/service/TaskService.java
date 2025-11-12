package com.securechat.webapi.service;

import com.securechat.webapi.entity.TaskEntity;
import com.securechat.webapi.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskEntity createTask(String title, String description, String createdBy, String assignee) {
        log.info("   └─ [SERVICE] TaskService.createTask() - Creating new task");
        log.info("      → Using TaskRepository (JPA/Hibernate)");
        
        TaskEntity task = new TaskEntity();
        task.setTitle(title);
        task.setDescription(description);
        task.setCreatedBy(createdBy);
        task.setAssignee(assignee);
        task.setStatus("todo");
        
        TaskEntity saved = taskRepository.save(task);
        log.info("      → Task saved with ID: {}", saved.getId());
        log.info("   └─ [SERVICE] TaskService.createTask() - Completed");
        
        return saved;
    }

    public List<TaskEntity> getAllTasks() {
        return taskRepository.findAllByOrderByUpdatedAtDesc();
    }

    public TaskEntity getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    @Transactional
    public TaskEntity updateTask(Long id, String title, String description, String status, String assignee) {
        TaskEntity task = getTaskById(id);
        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (status != null) task.setStatus(status);
        if (assignee != null) task.setAssignee(assignee);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
}




