package com.befapress.controller;

import com.befapress.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload APIs")
public class FileUploadController {

    private final FileStorageService fileStorageService;

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

        String filePath = fileStorageService.storeFile(file, "ads");

        Map<String, Object> response = new HashMap<>();
        response.put("url", filePath);
        response.put("type", isImage ? "image" : "video");
        response.put("originalName", file.getOriginalFilename());
        response.put("size", file.getSize());

        return ResponseEntity.ok(response);
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

        String filePath = fileStorageService.storeFile(file, "faces");

        Map<String, Object> response = new HashMap<>();
        response.put("url", filePath);
        response.put("type", "image");
        response.put("originalName", file.getOriginalFilename());

        return ResponseEntity.ok(response);
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
