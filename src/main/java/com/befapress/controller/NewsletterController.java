package com.befapress.controller;

import com.befapress.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
@Tag(name = "Newsletter", description = "Newsletter subscription APIs")
public class NewsletterController {

    // In a full implementation, this would use a repository
    // For now, using an in-memory set for demonstration
    private static final Set<String> subscribers = new HashSet<>();

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to newsletter")
    public ResponseEntity<MessageResponse> subscribe(@RequestParam @Email String email) {
        if (subscribers.contains(email.toLowerCase())) {
            return ResponseEntity.ok(MessageResponse.success("You are already subscribed to our newsletter."));
        }

        subscribers.add(email.toLowerCase());
        log.info("New newsletter subscriber: {}", email);

        // In a full implementation:
        // 1. Save to database
        // 2. Send confirmation email
        // 3. Add to email marketing service (MailChimp, SendGrid, etc.)

        return ResponseEntity.ok(MessageResponse.success("Successfully subscribed to BEFA Press newsletter!"));
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from newsletter")
    public ResponseEntity<MessageResponse> unsubscribe(@RequestParam @Email String email) {
        subscribers.remove(email.toLowerCase());
        log.info("Newsletter unsubscribe: {}", email);

        return ResponseEntity.ok(MessageResponse.success("Successfully unsubscribed from newsletter."));
    }

    @GetMapping("/status")
    @Operation(summary = "Check newsletter subscription status")
    public ResponseEntity<MessageResponse> checkStatus(@RequestParam @Email String email) {
        boolean isSubscribed = subscribers.contains(email.toLowerCase());
        String message = isSubscribed ? "You are subscribed to our newsletter." : "You are not subscribed.";
        return ResponseEntity.ok(MessageResponse.success(message));
    }
}
