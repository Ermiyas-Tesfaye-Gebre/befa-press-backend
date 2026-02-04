package com.befapress.controller;

import com.befapress.dto.request.RegisterDeviceTokenRequest;
import com.befapress.dto.response.MessageResponse;
import com.befapress.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Push Notifications", description = "FCM device token registration APIs")
public class PushNotificationController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/fcm-token")
    @Operation(summary = "Register FCM token for push notifications")
    public ResponseEntity<MessageResponse> registerToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }

        deviceTokenService.registerToken(
                userDetails.getUsername(),
                request.getToken(),
                request.getPlatform(),
                request.getDeviceName());

        return ResponseEntity.ok(MessageResponse.success("FCM token registered successfully"));
    }

    @DeleteMapping("/fcm-token")
    @Operation(summary = "Unregister FCM token")
    public ResponseEntity<MessageResponse> unregisterToken(
            @RequestParam String token,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }

        deviceTokenService.unregisterToken(token);
        return ResponseEntity.ok(MessageResponse.success("FCM token unregistered"));
    }

    @DeleteMapping("/fcm-token/all")
    @Operation(summary = "Unregister all FCM tokens for current user (logout from all devices)")
    public ResponseEntity<MessageResponse> unregisterAllTokens(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }

        deviceTokenService.unregisterAllTokensForUser(userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("All FCM tokens unregistered"));
    }
}
