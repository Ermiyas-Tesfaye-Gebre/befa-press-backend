package com.befapress.controller;

import com.befapress.dto.request.CreateCommentRequest;
import com.befapress.dto.response.CommentResponse;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment APIs for news and opinions")
public class CommentController {

    private final CommentService commentService;
    private final com.befapress.service.ReportService reportService;

    // ==================== NEWS COMMENTS ====================

    @GetMapping("/news/{newsId}")
    @Operation(summary = "Get comments for a news article")
    public ResponseEntity<PageResponse<CommentResponse>> getCommentsByNewsId(
            @PathVariable Long newsId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CommentResponse> response = commentService.getCommentsByNewsId(newsId, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/news/{newsId}")
    @Operation(summary = "Create a comment on a news article (guest or authenticated)")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long newsId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        CommentResponse response = commentService.createComment(newsId, request, userEmail, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/news/{newsId}/count")
    @Operation(summary = "Get comment count for a news article")
    public ResponseEntity<Long> getCommentCountByNewsId(@PathVariable Long newsId) {
        long count = commentService.getCommentCountByNewsId(newsId);
        return ResponseEntity.ok(count);
    }

    // ==================== OPINION COMMENTS ====================

    @GetMapping("/opinion/{opinionId}")
    @Operation(summary = "Get comments for an opinion article")
    public ResponseEntity<PageResponse<CommentResponse>> getCommentsByOpinionId(
            @PathVariable Long opinionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CommentResponse> response = commentService.getCommentsByOpinionId(opinionId, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/opinion/{opinionId}")
    @Operation(summary = "Create a comment on an opinion article (guest or authenticated)")
    public ResponseEntity<CommentResponse> createOpinionComment(
            @PathVariable Long opinionId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        CommentResponse response = commentService.createOpinionComment(opinionId, request, userEmail, httpRequest);
        return ResponseEntity.ok(response);
    }

    // ==================== REPLIES ====================

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies to a specific comment")
    public ResponseEntity<List<CommentResponse>> getReplies(@PathVariable Long commentId) {
        List<CommentResponse> replies = commentService.getReplies(commentId);
        return ResponseEntity.ok(replies);
    }

    // ==================== LIKE/UNLIKE ====================

    @PostMapping("/{commentId}/like")
    @Operation(summary = "Like a comment (authenticated users only)")
    public ResponseEntity<MessageResponse> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required to like comments"));
        }
        commentService.likeComment(commentId, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Comment liked"));
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "Unlike a comment (remove like)")
    public ResponseEntity<MessageResponse> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        commentService.unlikeComment(commentId, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Like removed"));
    }

    @GetMapping("/{commentId}/likes")
    @Operation(summary = "Get like count for a comment")
    public ResponseEntity<Integer> getLikeCount(@PathVariable Long commentId) {
        int count = commentService.getLikeCount(commentId);
        return ResponseEntity.ok(count);
    }

    // ==================== REPORT & DELETE ====================

    @PostMapping("/{commentId}/report")
    @Operation(summary = "Report a comment with a reason")
    public ResponseEntity<?> reportComment(
            @PathVariable Long commentId,
            @Valid @RequestBody com.befapress.dto.request.ReportCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String reporterEmail = userDetails != null ? userDetails.getUsername() : null;
        var response = reportService.createReport(commentId, request, reporterEmail, httpRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete own comment (authenticated users only)")
    public ResponseEntity<MessageResponse> deleteOwnComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        commentService.deleteOwnComment(commentId, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Comment deleted successfully"));
    }

    // ==================== RECENT COMMENTS ====================

    @GetMapping("/recent")
    @Operation(summary = "Get recent approved comments across all articles")
    public ResponseEntity<List<CommentResponse>> getRecentComments(
            @RequestParam(defaultValue = "10") int limit) {
        List<CommentResponse> comments = commentService.getRecentComments(limit);
        return ResponseEntity.ok(comments);
    }
}
