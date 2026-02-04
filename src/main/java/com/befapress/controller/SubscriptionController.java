package com.befapress.controller;

import com.befapress.dto.request.SubscribeRequest;
import com.befapress.dto.response.SubscriptionResponse;
import com.befapress.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints for subscription management
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Email subscription APIs")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to BEFA Press newsletter")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        SubscriptionResponse response = subscriptionService.subscribe(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Check subscription status by email")
    public ResponseEntity<SubscriptionResponse> getStatus(
            @RequestParam String email) {
        SubscriptionResponse response = subscriptionService.getStatus(email);
        return ResponseEntity.ok(response);
    }
}
