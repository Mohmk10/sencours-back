package com.sencours.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload un fichier vers Cloudinary
     * @param file le fichier a uploader
     * @param folder le dossier de destination (ex: "courses", "users")
     * @return l'URL publique du fichier
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String publicId = folder + "/" + UUID.randomUUID().toString();

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "sencours",
                    "resource_type", "auto",
                    "overwrite", true
            ));

            String url = (String) uploadResult.get("secure_url");
            log.info("Fichier uploade vers Cloudinary: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Echec de l'upload vers Cloudinary", e);
            throw new RuntimeException("Echec de l'upload du fichier: " + e.getMessage());
        }
    }

    /**
     * Upload une image avec transformation (redimensionnement)
     */
    public String uploadImageWithTransform(MultipartFile file, String folder, int width, int height) {
        try {
            String publicId = folder + "/" + UUID.randomUUID().toString();

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "sencours",
                    "resource_type", "auto",
                    "overwrite", true,
                    "transformation", ObjectUtils.asMap(
                            "width", width,
                            "height", height,
                            "crop", "fill",
                            "quality", "auto"
                    )
            ));

            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            log.error("Echec de l'upload vers Cloudinary", e);
            throw new RuntimeException("Echec de l'upload du fichier: " + e.getMessage());
        }
    }

    /**
     * Supprime un fichier de Cloudinary
     * @param publicId l'identifiant public du fichier
     */
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Fichier supprime de Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Echec de la suppression sur Cloudinary", e);
        }
    }

    /**
     * Extrait le public_id depuis une URL Cloudinary
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }
        // URL format: https://res.cloudinary.com/{cloud}/image/upload/v{version}/{folder}/{public_id}.{ext}
        String[] parts = cloudinaryUrl.split("/upload/");
        if (parts.length < 2) {
            return null;
        }
        String pathWithVersion = parts[1];
        // Retirer le v{version}/ prefix si present
        if (pathWithVersion.startsWith("v")) {
            int slashIndex = pathWithVersion.indexOf('/');
            if (slashIndex > 0) {
                pathWithVersion = pathWithVersion.substring(slashIndex + 1);
            }
        }
        // Retirer l'extension
        int lastDot = pathWithVersion.lastIndexOf('.');
        if (lastDot > 0) {
            pathWithVersion = pathWithVersion.substring(0, lastDot);
        }
        return pathWithVersion;
    }
}
