package com.sencours.service.impl;

import com.sencours.exception.BadRequestException;
import com.sencours.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov", "avi");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_PDF_EXTENSIONS = Set.of("pdf");

    private static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024; // 500 MB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;  // 10 MB
    private static final long MAX_PDF_SIZE = 50 * 1024 * 1024;    // 50 MB

    @Override
    public String storeFile(MultipartFile file, String type) {
        if (file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("Nom de fichier invalide");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();

        validateFile(file, type, extension);

        String subDir = type.toLowerCase() + "s";
        Path uploadPath = Paths.get(uploadDir, subDir);

        try {
            Files.createDirectories(uploadPath);

            String newFilename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadPath.resolve(newFilename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return baseUrl + "/uploads/" + subDir + "/" + newFilename;

        } catch (IOException e) {
            throw new BadRequestException("Erreur lors de l'upload du fichier: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/uploads/")) {
            return;
        }

        try {
            String relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + 9);
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Logged but does not fail the operation
        }
    }

    private void validateFile(MultipartFile file, String type, String extension) {
        switch (type.toLowerCase()) {
            case "video":
                if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
                    throw new BadRequestException("Extension vidéo non autorisée. Utilisez: " + ALLOWED_VIDEO_EXTENSIONS);
                }
                if (file.getSize() > MAX_VIDEO_SIZE) {
                    throw new BadRequestException("La vidéo ne doit pas dépasser 500 MB");
                }
                break;
            case "image":
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                    throw new BadRequestException("Extension image non autorisée. Utilisez: " + ALLOWED_IMAGE_EXTENSIONS);
                }
                if (file.getSize() > MAX_IMAGE_SIZE) {
                    throw new BadRequestException("L'image ne doit pas dépasser 10 MB");
                }
                break;
            case "pdf":
                if (!ALLOWED_PDF_EXTENSIONS.contains(extension)) {
                    throw new BadRequestException("Seuls les fichiers PDF sont autorisés");
                }
                if (file.getSize() > MAX_PDF_SIZE) {
                    throw new BadRequestException("Le PDF ne doit pas dépasser 50 MB");
                }
                break;
            default:
                throw new BadRequestException("Type de fichier non reconnu: " + type);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
