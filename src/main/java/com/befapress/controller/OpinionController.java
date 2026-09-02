package com.befapress.controller;

import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.OpinionResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.ArticleLikeService;
import com.befapress.service.OpinionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/opinions")
@RequiredArgsConstructor
@Tag(name = "Opinions", description = "Public opinion articles APIs")
public class OpinionController {

    private final OpinionService opinionService;
    private final ArticleLikeService articleLikeService;

    @GetMapping
    @Operation(summary = "Get all published opinions with pagination")
    public ResponseEntity<PageResponse<OpinionResponse>> getAllOpinions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<OpinionResponse> response = opinionService.getAllPublishedOpinions(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opinion by ID")
    public ResponseEntity<OpinionResponse> getOpinionById(@PathVariable Long id) {
        OpinionResponse response = opinionService.getOpinionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get opinion by slug")
    public ResponseEntity<OpinionResponse> getOpinionBySlug(@PathVariable String slug) {
        OpinionResponse response = opinionService.getOpinionBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "Increment view count for opinion")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        opinionService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured opinions")
    public ResponseEntity<List<OpinionResponse>> getFeaturedOpinions() {
        List<OpinionResponse> response = opinionService.getFeaturedOpinions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get opinions by author")
    public ResponseEntity<PageResponse<OpinionResponse>> getOpinionsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<OpinionResponse> response = opinionService.getOpinionsByAuthor(authorId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search opinions by title or content")
    public ResponseEntity<PageResponse<OpinionResponse>> searchOpinions(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<OpinionResponse> response = opinionService.searchOpinions(q, page, size);
        return ResponseEntity.ok(response);
    }

    // ==================== LIKE ENDPOINTS ====================

    @PostMapping("/{id}/like")
    @Operation(summary = "Like an opinion")
    public ResponseEntity<MessageResponse> likeOpinion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        articleLikeService.likeOpinion(id, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Opinion liked"));
    }

    @DeleteMapping("/{id}/like")
    @Operation(summary = "Unlike an opinion")
    public ResponseEntity<MessageResponse> unlikeOpinion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        articleLikeService.unlikeOpinion(id, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Like removed"));
    }

    @GetMapping("/{id}/like-status")
    @Operation(summary = "Get like status and count for an opinion")
    public ResponseEntity<Map<String, Object>> getOpinionLikeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        long count = articleLikeService.getOpinionLikeCount(id);
        boolean liked = userDetails != null && articleLikeService.isOpinionLiked(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("count", count, "liked", liked));
    }
}
