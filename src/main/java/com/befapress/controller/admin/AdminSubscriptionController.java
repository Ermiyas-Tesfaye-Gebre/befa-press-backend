package com.befapress.controller.admin;

import com.befapress.dto.response.SubscriptionResponse;
import com.befapress.entity.SubscriptionStatus;
import com.befapress.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for subscription management
 */
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin - Subscriptions", description = "Admin subscription management APIs")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Get all subscriptions (paginated)")
    public ResponseEntity<Page<SubscriptionResponse>> getAllSubscriptions(Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(pageable));
    }

    @PutMapping("/{id}/extend")
    @Operation(summary = "Extend subscription by days")
    public ResponseEntity<SubscriptionResponse> extendSubscription(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(subscriptionService.extendSubscription(id, days));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update subscription status")
    public ResponseEntity<SubscriptionResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.updateStatus(id, status));
    }

    @PostMapping("/send-promotional")
    @Operation(summary = "Manually trigger promotional emails to active subscribers")
    public ResponseEntity<Map<String, String>> sendPromotionalEmails() {
        subscriptionService.sendWeeklyPromotionalEmails();
        return ResponseEntity.ok(Map.of("message", "Promotional emails sent to active subscribers"));
    }
}
