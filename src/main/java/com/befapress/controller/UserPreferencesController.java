package com.befapress.controller;

import com.befapress.dto.request.UpdatePreferencesRequest;
import com.befapress.dto.response.UserPreferencesResponse;
import com.befapress.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "User settings and preferences APIs")
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;

    @GetMapping
    @Operation(summary = "Get current user preferences")
    public ResponseEntity<UserPreferencesResponse> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        UserPreferencesResponse response = preferencesService.getPreferences(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update user preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        UserPreferencesResponse response = preferencesService.updatePreferences(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
