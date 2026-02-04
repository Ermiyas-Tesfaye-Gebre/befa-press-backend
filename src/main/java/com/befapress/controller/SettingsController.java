package com.befapress.controller;

import com.befapress.dto.request.UpdateSettingsRequest;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.SiteSettingsResponse;
import com.befapress.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Site configuration APIs")
public class SettingsController {

    private final SettingsService settingsService;

    // ========== Admin Endpoints ==========

    @GetMapping("/admin/settings")
    @Operation(summary = "Get all site settings (Admin only)")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SiteSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/admin/settings")
    @Operation(summary = "Update site settings (Super Admin only)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SiteSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String updatedBy = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(settingsService.updateSettings(request, updatedBy));
    }

    @GetMapping("/admin/settings/timezones")
    @Operation(summary = "Get available timezones")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<String>> getTimezones() {
        return ResponseEntity.ok(settingsService.getAvailableTimezones());
    }

    @GetMapping("/admin/settings/languages")
    @Operation(summary = "Get supported languages")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, String>>> getLanguages() {
        return ResponseEntity.ok(settingsService.getSupportedLanguages());
    }

    // ========== Public Endpoints ==========

    @GetMapping("/public/settings")
    @Operation(summary = "Get public site settings (branding, social, etc.)")
    public ResponseEntity<Map<String, Object>> getPublicSettings() {
        return ResponseEntity.ok(settingsService.getPublicSettings());
    }
}
