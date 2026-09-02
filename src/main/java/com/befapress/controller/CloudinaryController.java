package com.befapress.controller;

import com.befapress.dto.response.MessageResponse;
import com.befapress.entity.CloudinaryConfig;
import com.befapress.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cloudinary")
@RequiredArgsConstructor
@Tag(name = "Cloudinary", description = "Cloudinary cloud storage configuration APIs")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @GetMapping("/config")
    @Operation(summary = "Get current Cloudinary configuration")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(cloudinaryService.getConfigSafe());
    }

    @PutMapping("/config")
    @Operation(summary = "Update Cloudinary configuration")
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> configData) {
        try {
            CloudinaryConfig saved = cloudinaryService.saveConfig(configData);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cloudinary configuration saved successfully!",
                    "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Failed to save config: " + e.getMessage()));
        }
    }

    @PostMapping("/test")
    @Operation(summary = "Test Cloudinary connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = cloudinaryService.testConnection();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/usage")
    @Operation(summary = "Get Cloudinary storage usage stats")
    public ResponseEntity<Map<String, Object>> getUsage() {
        Map<String, Object> result = cloudinaryService.testConnection();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @Operation(summary = "Check if Cloudinary is enabled")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean enabled = cloudinaryService.isEnabled();
        return ResponseEntity.ok(Map.of(
                "enabled", enabled,
                "message", enabled ? "Cloudinary is active" : "Cloudinary is not configured or disabled"));
    }
}
