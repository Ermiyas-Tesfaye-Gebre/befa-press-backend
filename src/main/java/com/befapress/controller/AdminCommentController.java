package com.befapress.controller;

import com.befapress.dto.response.CommentResponse;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
@Tag(name = "Admin - Comments", description = "Comment moderation APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EDITOR')")
public class AdminCommentController {

    private final CommentService commentService;
    private final com.befapress.service.ReportService reportService;

    @GetMapping("/pending")
    @Operation(summary = "Get pending comments for moderation")
    public ResponseEntity<PageResponse<CommentResponse>> getPendingComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CommentResponse> response = commentService.getPendingComments(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reported")
    @Operation(summary = "Get reported comments")
    public ResponseEntity<PageResponse<CommentResponse>> getReportedComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CommentResponse> response = commentService.getReportedComments(page, size);
        return ResponseEntity.ok(response);
    }

    // === USER REPORTS (with reasons) ===

    @GetMapping("/reports")
    @Operation(summary = "Get all user reports")
    public ResponseEntity<PageResponse<com.befapress.dto.response.ReportResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var response = reportService.getPendingReports(page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Resolve a user report (APPROVE or REJECT)")
    public ResponseEntity<com.befapress.dto.response.ReportResponse> resolveReport(
            @PathVariable Long reportId,
            @RequestParam String resolution,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "unknown";
        var response = reportService.resolveReport(reportId, resolution, adminEmail);
        return ResponseEntity.ok(response);
    }

    // === COMMENT ACTIONS ===

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a comment")
    public ResponseEntity<CommentResponse> approveComment(@PathVariable Long id) {
        CommentResponse response = commentService.approveComment(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a comment")
    public ResponseEntity<CommentResponse> rejectComment(@PathVariable Long id) {
        CommentResponse response = commentService.rejectComment(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a comment permanently")
    public ResponseEntity<MessageResponse> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok(MessageResponse.success("Comment deleted successfully"));
    }
}
