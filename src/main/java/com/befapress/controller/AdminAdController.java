package com.befapress.controller;

import com.befapress.dto.AdDto;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ads")
@RequiredArgsConstructor
@Tag(name = "Admin - Ads", description = "Ad Management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EDITOR')")
public class AdminAdController {

    private final AdService adService;

    @GetMapping
    @Operation(summary = "Get all ads")
    public ResponseEntity<PageResponse<AdDto>> getAllAds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adService.getAllAds(page, size));
    }

    @PostMapping
    @Operation(summary = "Create a new ad")
    public ResponseEntity<AdDto> createAd(@RequestBody AdDto request) {
        return ResponseEntity.ok(adService.createAd(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an ad")
    public ResponseEntity<AdDto> updateAd(
            @PathVariable Long id,
            @RequestBody AdDto request) {
        return ResponseEntity.ok(adService.updateAd(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an ad")
    public ResponseEntity<MessageResponse> deleteAd(@PathVariable Long id) {
        adService.deleteAd(id);
        return ResponseEntity.ok(MessageResponse.success("Ad deleted successfully"));
    }
}
