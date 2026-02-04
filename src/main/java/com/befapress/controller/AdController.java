package com.befapress.controller;

import com.befapress.dto.AdDto;
import com.befapress.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "Ads", description = "Public Ad APIs")
public class AdController {

    private final AdService adService;

    @GetMapping("/fetch")
    @Operation(summary = "Get an active ad for a zone")
    public ResponseEntity<AdDto> getAdForZone(@RequestParam String zone) {
        AdDto ad = adService.getAdForZone(zone);
        if (ad == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ad);
    }

    @GetMapping("/{id}/click")
    @Operation(summary = "Track ad click and redirect")
    public ResponseEntity<Void> trackClick(@PathVariable Long id) {
        adService.trackClick(id);
        // In real world, we would redirect. Here we just track.
        // The frontend will handle the redirection based on targetUrl
        return ResponseEntity.ok().build();
    }
}
