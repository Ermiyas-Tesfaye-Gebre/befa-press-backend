package com.befapress.controller;

import com.befapress.dto.dashboard.DashboardStatsDTO;
import com.befapress.dto.response.ActivityLogResponse;
import com.befapress.entity.ActivityLog;
import com.befapress.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "APIs for Admin Dashboard metrics and activity")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get aggregated dashboard statistics")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/activity")
    @Operation(summary = "Get recent system activity")
    public ResponseEntity<List<ActivityLogResponse>> getRecentActivity() {
        List<ActivityLog> logs = dashboardService.getRecentActivity();
        List<ActivityLogResponse> response = logs.stream()
                .map(log -> ActivityLogResponse.builder()
                        .id(log.getId())
                        .type(log.getType())
                        .message(log.getMessage())
                        .actor(log.getActor())
                        .createdAt(log.getCreatedAt())
                        .relatedId(log.getRelatedId())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
