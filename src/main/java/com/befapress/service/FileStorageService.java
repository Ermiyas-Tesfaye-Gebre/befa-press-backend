package com.befapress.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            // Create subdirectories for different types
            Files.createDirectories(this.fileStorageLocation.resolve("ads"));
            Files.createDirectories(this.fileStorageLocation.resolve("news"));
            Files.createDirectories(this.fileStorageLocation.resolve("profiles"));
            Files.createDirectories(this.fileStorageLocation.resolve("chat"));
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    /**
     * Store file with auto-generated unique name
     * 
     * @param file      MultipartFile to store
     * @param subfolder Subfolder like "ads", "news", "profiles"
     * @return Relative path to the stored file (e.g., "/uploads/ads/abc123.jpg")
     */
    public String storeFile(MultipartFile file, String subfolder) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        // Generate unique filename
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            if (newFilename.contains("..")) {
                throw new RuntimeException("Invalid file path: " + newFilename);
            }

            Path targetLocation = this.fileStorageLocation.resolve(subfolder).resolve(newFilename);
            // Ensure the directory exists
            Files.createDirectories(targetLocation.getParent());

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored file: {}", targetLocation);
            return "/uploads/" + subfolder + "/" + newFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + newFilename, ex);
        }
    }

    /**
     * Delete a file by its path (relative or filename)
     */
    public boolean deleteFile(String filePath) {
        try {
            // Strip /uploads/ prefix if present
            if (filePath.startsWith("/uploads/")) {
                filePath = filePath.substring("/uploads/".length());
            }
            Path targetLocation = this.fileStorageLocation.resolve(filePath).normalize();
            return Files.deleteIfExists(targetLocation);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", filePath, ex);
            return false;
        }
    }

    /**
     * Get the storage path
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
