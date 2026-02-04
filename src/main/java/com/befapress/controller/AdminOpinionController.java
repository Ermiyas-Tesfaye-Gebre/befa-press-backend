package com.befapress.controller;

import com.befapress.dto.response.*;
import com.befapress.service.OpinionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/opinions")
@RequiredArgsConstructor
@Tag(name = "Admin - Opinions", description = "Admin opinion management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EDITOR')")
public class AdminOpinionController {

    private final OpinionService opinionService;

    @GetMapping
    @Operation(summary = "Get all opinions with optional status filter")
    public ResponseEntity<PageResponse<OpinionResponse>> getAllOpinions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        PageResponse<OpinionResponse> response = opinionService.getAdminOpinionsList(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending opinions for approval")
    public ResponseEntity<PageResponse<OpinionResponse>> getPendingOpinions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<OpinionResponse> response = opinionService.getAdminOpinionsList(page, size, "PENDING");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opinion by ID")
    public ResponseEntity<OpinionResponse> getOpinionById(@PathVariable Long id) {
        OpinionResponse response = opinionService.getOpinionById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve an opinion for publishing")
    public ResponseEntity<OpinionResponse> approveOpinion(@PathVariable Long id) {
        OpinionResponse response = opinionService.approveOpinion(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject an opinion")
    public ResponseEntity<OpinionResponse> rejectOpinion(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        OpinionResponse response = opinionService.rejectOpinion(id, reason);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/featured")
    @Operation(summary = "Set opinion as featured")
    public ResponseEntity<OpinionResponse> setFeatured(
            @PathVariable Long id,
            @RequestParam boolean featured) {
        OpinionResponse response = opinionService.setFeatured(id, featured);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an opinion (soft delete)")
    public ResponseEntity<MessageResponse> deleteOpinion(@PathVariable Long id) {
        opinionService.deleteOpinion(id);
        return ResponseEntity.ok(MessageResponse.success("Opinion deleted successfully"));
    }
}
