package com.befapress.controller;

import com.befapress.service.CloudinaryService;
import com.befapress.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import com.befapress.service.FaceVerificationService;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload APIs — uses Cloudinary when enabled, local storage as fallback")
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    private final FaceVerificationService faceVerificationService;

    // Allowed MIME types for images and videos
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp",
            "image/webp", "image/heic", "image/heif", "image/tiff", "image/svg+xml",
            "image/apng", "image/x-icon"
    };

    private static final String[] ALLOWED_VIDEO_TYPES = {
            "video/mp4", "video/webm", "video/ogg", "video/avi", "video/quicktime",
            "video/x-msvideo", "video/x-matroska", "video/x-flv", "video/3gpp"
    };

    @PostMapping("/ad-media")
    @Operation(summary = "Upload ad media (image or video)")
    public ResponseEntity<?> uploadAdMedia(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String contentType = file.getContentType();
        boolean isImage = isAllowedType(contentType, ALLOWED_IMAGE_TYPES);
        boolean isVideo = isAllowedType(contentType, ALLOWED_VIDEO_TYPES);

        if (!isImage && !isVideo) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "File type not supported. Allowed: images (JPEG, PNG, GIF, WEBP, etc.) and videos (MP4, WEBM, etc.)"));
        }

        // Max file size check (50MB for videos, 10MB for images)
        long maxSize = isVideo ? 50 * 1024 * 1024 : 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("File too large. Max size: %dMB", maxSize / (1024 * 1024))));
        }

        try {
            String filePath;
            String storageType;

            if (cloudinaryService.isEnabled()) {
                // Upload to Cloudinary
                filePath = cloudinaryService.uploadFile(file, "ads");
                storageType = "cloudinary";
                log.info("Ad media uploaded to Cloudinary: {}", filePath);
            } else {
                // Fallback to local storage
                filePath = fileStorageService.storeFile(file, "ads");
                storageType = "local";
                log.info("Ad media uploaded locally: {}", filePath);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("url", filePath);
            response.put("type", isImage ? "image" : "video");
            response.put("originalName", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("storageType", storageType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload ad media", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/news-media")
    @Operation(summary = "Upload news article media (image or video)")
    public ResponseEntity<?> uploadNewsMedia(@RequestParam("file") MultipartFile file) {
        return uploadMedia(file, "news", ALLOWED_IMAGE_TYPES, ALLOWED_VIDEO_TYPES);
    }

    @PostMapping("/face-verification")
    @Operation(summary = "Upload face verification image")
    public ResponseEntity<?> uploadFaceVerification(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String contentType = file.getContentType();
        if (!isAllowedType(contentType, ALLOWED_IMAGE_TYPES)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "File type not supported. Allowed: images (JPEG, PNG, WEBP, etc.)"));
        }

        // Max file size check (5MB for face images)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("File too large. Max size: %dMB", maxSize / (1024 * 1024))));
        }

        java.nio.file.Path tempFile = null;
        try {
            // Cache bytes once — avoids MultipartFile stream-consumption issues
            byte[] imageBytes = file.getBytes();

            // Write to a temp file to run Python face verification
            tempFile = java.nio.file.Files.createTempFile("face_verify_", ".jpg");
            java.nio.file.Files.write(tempFile, imageBytes);

            // Run face verification and get descriptor
            String faceDescriptor = faceVerificationService.extractFaceDescriptor(tempFile);

            String filePath;
            String storageType;

            if (cloudinaryService.isEnabled()) {
                filePath = cloudinaryService.uploadFile(file, "faces");
                storageType = "cloudinary";
            } else {
                // Write permanent file from cached bytes (don't re-read MultipartFile)
                String originalFilename = org.springframework.util.StringUtils.cleanPath(
                        file.getOriginalFilename() != null ? file.getOriginalFilename() : "face.jpg");
                String ext = "";
                int dotIdx = originalFilename.lastIndexOf('.');
                if (dotIdx > 0) ext = originalFilename.substring(dotIdx);
                String newFilename = java.util.UUID.randomUUID().toString() + ext;

                java.nio.file.Path facesDir = fileStorageService.getFileStorageLocation().resolve("faces");
                java.nio.file.Files.createDirectories(facesDir);
                java.nio.file.Path targetPath = facesDir.resolve(newFilename);
                java.nio.file.Files.write(targetPath, imageBytes);

                filePath = "/uploads/faces/" + newFilename;
                storageType = "local";
                log.info("Stored face verification image: {}", targetPath);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("url", filePath);
            response.put("type", "image");
            response.put("originalName", file.getOriginalFilename());
            response.put("storageType", storageType);
            response.put("faceDescriptor", faceDescriptor);

            return ResponseEntity.ok(response);
        } catch (com.befapress.exception.BadRequestException e) {
            log.warn("Face verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to upload face verification image", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Face verification failed: " + e.getMessage()));
        } finally {
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Generic media upload handler for any subfolder.
     */
    private ResponseEntity<?> uploadMedia(MultipartFile file, String subfolder,
            String[] allowedImageTypes, String[] allowedVideoTypes) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String contentType = file.getContentType();
        boolean isImage = isAllowedType(contentType, allowedImageTypes);
        boolean isVideo = isAllowedType(contentType, allowedVideoTypes);

        if (!isImage && !isVideo) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "File type not supported"));
        }

        long maxSize = isVideo ? 50 * 1024 * 1024 : 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("File too large. Max size: %dMB", maxSize / (1024 * 1024))));
        }

        try {
            String filePath;
            String storageType;

            if (cloudinaryService.isEnabled()) {
                filePath = cloudinaryService.uploadFile(file, subfolder);
                storageType = "cloudinary";
            } else {
                filePath = fileStorageService.storeFile(file, subfolder);
                storageType = "local";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("url", filePath);
            response.put("type", isImage ? "image" : "video");
            response.put("originalName", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("storageType", storageType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload media to {}", subfolder, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Upload failed: " + e.getMessage()));
        }
    }

    private boolean isAllowedType(String contentType, String[] allowedTypes) {
        if (contentType == null)
            return false;
        for (String type : allowedTypes) {
            if (contentType.equalsIgnoreCase(type))
                return true;
        }
        return false;
    }
}
