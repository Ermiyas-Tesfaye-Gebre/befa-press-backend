package com.befapress.controller;

import com.befapress.dto.request.SocialConfigRequest;
import com.befapress.dto.response.MessageResponse;
import com.befapress.entity.SocialPlatformConfig;
import com.befapress.entity.SocialShareQueue;
import com.befapress.repository.SocialPlatformConfigRepository;
import com.befapress.repository.SocialShareQueueRepository;
import com.befapress.service.social.SocialShareService;
import com.befapress.service.social.TelegramPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/social")
@RequiredArgsConstructor
@Tag(name = "Social Media Admin", description = "Admin APIs for social media configuration")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SocialMediaController {

    private final SocialPlatformConfigRepository configRepository;
    private final SocialShareQueueRepository queueRepository;
    private final SocialShareService shareService;
    private final TelegramPublisher telegramPublisher;
    private final com.befapress.service.social.FacebookPublisher facebookPublisher;
    private final ObjectMapper objectMapper;

    // ========== Configuration Management ==========

    @GetMapping("/platforms")
    @Operation(summary = "Get all platform configurations")
    public ResponseEntity<List<SocialPlatformConfig>> getAllPlatforms() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @GetMapping("/platforms/{platform}")
    @Operation(summary = "Get configuration for a specific platform")
    public ResponseEntity<SocialPlatformConfig> getPlatform(@PathVariable String platform) {
        SocialPlatformConfig.Platform p = SocialPlatformConfig.Platform.valueOf(platform.toUpperCase());
        return configRepository.findByPlatform(p)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/platforms/telegram")
    @Operation(summary = "Configure Telegram bot")
    public ResponseEntity<?> configureTelegram(@RequestBody SocialConfigRequest request) {
        try {
            SocialPlatformConfig config = configRepository
                    .findByPlatform(SocialPlatformConfig.Platform.TELEGRAM)
                    .orElse(SocialPlatformConfig.builder()
                            .platform(SocialPlatformConfig.Platform.TELEGRAM)
                            .build());

            Map<String, String> credentials = new HashMap<>();
            credentials.put("botToken", request.getBotToken());
            credentials.put("channelId", request.getChannelId());

            config.setCredentials(objectMapper.writeValueAsString(credentials));
            config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            config.setChannelUrl(request.getChannelUrl());
            config.setShareNews(request.getShareNews() != null ? request.getShareNews() : true);
            config.setShareOpinions(request.getShareOpinions() != null ? request.getShareOpinions() : true);

            if (request.getMessageTemplate() != null) {
                config.setMessageTemplate(request.getMessageTemplate());
            }

            configRepository.save(config);
            return ResponseEntity.ok(MessageResponse.success("Telegram configured successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Failed to configure Telegram: " + e.getMessage()));
        }
    }

    @PutMapping("/platforms/{platform}/toggle")
    @Operation(summary = "Enable or disable a platform")
    public ResponseEntity<?> togglePlatform(
            @PathVariable String platform,
            @RequestParam boolean enabled) {
        SocialPlatformConfig.Platform p = SocialPlatformConfig.Platform.valueOf(platform.toUpperCase());

        return configRepository.findByPlatform(p)
                .map(config -> {
                    config.setEnabled(enabled);
                    configRepository.save(config);
                    return ResponseEntity.ok(MessageResponse.success(
                            platform + " " + (enabled ? "enabled" : "disabled")));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Connection Testing ==========

    @PostMapping("/platforms/telegram/test")
    @Operation(summary = "Test Telegram bot connection")
    public ResponseEntity<?> testTelegram() {
        boolean success = telegramPublisher.testConnection();
        if (success) {
            return ResponseEntity.ok(MessageResponse.success("Telegram connection successful!"));
        } else {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Telegram connection failed. Check credentials."));
        }
    }

    @PostMapping("/platforms/facebook")
    @Operation(summary = "Configure Facebook Page")
    public ResponseEntity<?> configureFacebook(@RequestBody SocialConfigRequest request) {
        try {
            SocialPlatformConfig config = configRepository
                    .findByPlatform(SocialPlatformConfig.Platform.FACEBOOK)
                    .orElse(SocialPlatformConfig.builder()
                            .platform(SocialPlatformConfig.Platform.FACEBOOK)
                            .build());

            Map<String, String> credentials = new HashMap<>();

            // Preserve existing credentials (like appId/appSecret)
            if (config.getCredentials() != null) {
                try {
                    Map<String, String> existing = objectMapper.readValue(config.getCredentials(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                            });
                    credentials.putAll(existing);
                } catch (Exception ignored) {
                }
            }

            credentials.put("pageId", request.getPageId());
            credentials.put("accessToken", request.getAccessToken());

            config.setCredentials(objectMapper.writeValueAsString(credentials));
            config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            config.setChannelUrl(request.getChannelUrl());
            config.setShareNews(request.getShareNews() != null ? request.getShareNews() : true);
            config.setShareOpinions(request.getShareOpinions() != null ? request.getShareOpinions() : true);

            if (request.getMessageTemplate() != null) {
                config.setMessageTemplate(request.getMessageTemplate());
            }

            configRepository.save(config);
            return ResponseEntity.ok(MessageResponse.success("Facebook configured successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Failed to configure Facebook: " + e.getMessage()));
        }
    }

    @PostMapping("/platforms/facebook/test")
    @Operation(summary = "Test Facebook Page connection")
    public ResponseEntity<?> testFacebook() {
        try {
            String result = facebookPublisher.publish("BEFA Press Connection Test", "https://befapress.com");
            if (result != null) {
                return ResponseEntity.ok(MessageResponse.success("Facebook test post successful! Post ID: " + result));
            } else {
                return ResponseEntity.badRequest()
                        .body(MessageResponse.error("Facebook connection failed. Check Page ID and Access Token."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Facebook test failed: " + e.getMessage()));
        }
    }

    // ========== Share Queue Management ==========

    @GetMapping("/queue")
    @Operation(summary = "Get share queue with pagination")
    public ResponseEntity<Page<SocialShareQueue>> getQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (status != null) {
            SocialShareQueue.ShareStatus s = SocialShareQueue.ShareStatus.valueOf(status.toUpperCase());
            // Would need a custom query, for now return all
        }

        return ResponseEntity.ok(queueRepository.findAll(pageable));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get share statistics")
    public ResponseEntity<SocialShareService.ShareStats> getStats() {
        return ResponseEntity.ok(shareService.getStats());
    }

    @PostMapping("/queue/{id}/retry")
    @Operation(summary = "Retry a failed share")
    public ResponseEntity<?> retryShare(@PathVariable Long id) {
        return queueRepository.findById(id)
                .map(queue -> {
                    if (queue.getStatus() == SocialShareQueue.ShareStatus.FAILED) {
                        queue.setStatus(SocialShareQueue.ShareStatus.PENDING);
                        queueRepository.save(queue);
                        shareService.processQueue();
                        return ResponseEntity.ok(MessageResponse.success("Retry initiated"));
                    }
                    return ResponseEntity.badRequest().body(MessageResponse.error("Can only retry failed shares"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/queue/{id}")
    @Operation(summary = "Cancel a pending share")
    public ResponseEntity<?> cancelShare(@PathVariable Long id) {
        return queueRepository.findById(id)
                .map(queue -> {
                    if (queue.getStatus() == SocialShareQueue.ShareStatus.PENDING) {
                        queue.setStatus(SocialShareQueue.ShareStatus.CANCELLED);
                        queueRepository.save(queue);
                        return ResponseEntity.ok(MessageResponse.success("Share cancelled"));
                    }
                    return ResponseEntity.badRequest().body(MessageResponse.error("Can only cancel pending shares"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Manual Share ==========

    @PostMapping("/share/test")
    @Operation(summary = "Send a test message to all enabled platforms")
    public ResponseEntity<?> sendTestMessage(@RequestParam(defaultValue = "BEFA Press Test") String message) {
        try {
            telegramPublisher.publish("Test Message", message, "https://befapress.com", null);
            return ResponseEntity.ok(MessageResponse.success("Test message sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Failed to send test: " + e.getMessage()));
        }
    }
}
