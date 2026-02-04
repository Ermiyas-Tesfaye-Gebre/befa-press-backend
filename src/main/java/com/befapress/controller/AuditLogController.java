package com.befapress.controller;

import com.befapress.dto.response.AuditLogResponse;
import com.befapress.entity.AuditLog;
import com.befapress.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "APIs for viewing audit logs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'AUDITOR')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "List all audit logs (paginated)")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ResponseEntity.ok(logs.map(this::mapToResponse));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, pageable);
        return ResponseEntity.ok(logs.map(this::mapToResponse));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action type")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByAction(
            @PathVariable String action,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
        return ResponseEntity.ok(logs.map(this::mapToResponse));
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .performedById(log.getUser() != null ? log.getUser().getId() : null)
                .performedByName(log.getPerformedByName())
                .targetUserId(log.getTargetUserId())
                .targetUserName(log.getTargetUserName())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
