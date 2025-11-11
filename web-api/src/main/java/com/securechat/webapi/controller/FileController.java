package com.securechat.webapi.controller;

import com.securechat.webapi.entity.FileEntity;
import com.securechat.webapi.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:5173")
public class FileController {
    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/meta")
    public ResponseEntity<FileEntity> createFileMetadata(
            @RequestParam String filename,
            @RequestParam String originalFilename,
            @RequestParam(required = false) String contentType,
            @RequestParam Long fileSize,
            @RequestParam String uploader,
            @RequestParam(required = false) String checksum,
            @RequestParam(required = false) String filePath) {
        FileEntity file = fileService.createFileMetadata(
                filename, originalFilename, contentType, fileSize, uploader, checksum, filePath);
        return ResponseEntity.status(HttpStatus.CREATED).body(file);
    }

    @GetMapping
    public ResponseEntity<List<FileEntity>> getAllFiles() {
        List<FileEntity> files = fileService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileEntity> getFile(@PathVariable Long id) {
        try {
            FileEntity file = fileService.getFileById(id);
            return ResponseEntity.ok(file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

