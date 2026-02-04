package com.befapress.controller;

import com.befapress.dto.response.LoginHistoryResponse;
import com.befapress.entity.LoginHistory;
import com.befapress.repository.LoginHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/login-history")
@RequiredArgsConstructor
@Tag(name = "Login History", description = "APIs for viewing login history")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'AUDITOR')")
public class LoginHistoryController {

    private final LoginHistoryRepository loginHistoryRepository;

    @GetMapping
    @Operation(summary = "List all login attempts (paginated)")
    public ResponseEntity<Page<LoginHistoryResponse>> getAllHistory(Pageable pageable) {
        Page<LoginHistory> history = loginHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ResponseEntity.ok(history.map(this::mapToResponse));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get login history for a specific user")
    public ResponseEntity<Page<LoginHistoryResponse>> getHistoryByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<LoginHistory> history = loginHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return ResponseEntity.ok(history.map(this::mapToResponse));
    }

    private LoginHistoryResponse mapToResponse(LoginHistory history) {
        return LoginHistoryResponse.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .userEmail(history.getUserEmail())
                .ipAddress(history.getIpAddress())
                .userAgent(history.getUserAgent())
                .status(history.getStatus())
                .failureReason(history.getFailureReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
