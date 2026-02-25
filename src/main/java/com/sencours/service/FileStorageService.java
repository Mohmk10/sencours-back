package com.sencours.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String type);
    void deleteFile(String fileUrl);
}
