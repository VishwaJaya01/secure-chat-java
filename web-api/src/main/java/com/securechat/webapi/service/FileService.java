package com.securechat.webapi.service;

import com.securechat.webapi.entity.FileEntity;
import com.securechat.webapi.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FileService {
    private final FileRepository fileRepository;

    @Autowired
    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Transactional
    public FileEntity createFileMetadata(String filename, String originalFilename, String contentType, 
                                         Long fileSize, String uploader, String checksum, String filePath) {
        FileEntity file = new FileEntity();
        file.setFilename(filename);
        file.setOriginalFilename(originalFilename);
        file.setContentType(contentType);
        file.setFileSize(fileSize);
        file.setUploader(uploader);
        file.setChecksum(checksum);
        file.setFilePath(filePath);
        return fileRepository.save(file);
    }

    public List<FileEntity> getAllFiles() {
        return fileRepository.findAllByOrderByCreatedAtDesc();
    }

    public FileEntity getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
    }
}

