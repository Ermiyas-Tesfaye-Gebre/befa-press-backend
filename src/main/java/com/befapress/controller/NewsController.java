package com.befapress.controller;

import com.befapress.dto.request.CreateNewsRequest;
import com.befapress.dto.response.*;
import com.befapress.service.ArticleLikeService;
import com.befapress.service.NewsService;
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
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "Public news APIs")
public class NewsController {

    private final NewsService newsService;
    private final ArticleLikeService articleLikeService;

    @GetMapping
    @Operation(summary = "Get all published news with pagination")
    public ResponseEntity<PageResponse<NewsListResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy) {
        PageResponse<NewsListResponse> response = newsService.getAllPublishedNews(page, size, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get news by ID")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        NewsResponse response = newsService.getNewsById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get news by slug")
    public ResponseEntity<NewsResponse> getNewsBySlug(@PathVariable String slug) {
        NewsResponse response = newsService.getNewsBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "Increment view count for news article")
    public ResponseEntity<MessageResponse> incrementViewCount(@PathVariable Long id) {
        newsService.incrementViewCount(id);
        return ResponseEntity.ok(MessageResponse.success("View count incremented"));
    }

    @GetMapping("/breaking")
    @Operation(summary = "Get breaking news")
    public ResponseEntity<List<NewsListResponse>> getBreakingNews(
            @RequestParam(defaultValue = "5") int limit) {
        List<NewsListResponse> response = newsService.getBreakingNews(limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured news")
    public ResponseEntity<List<NewsListResponse>> getFeaturedNews() {
        List<NewsListResponse> response = newsService.getFeaturedNews();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending news")
    public ResponseEntity<List<NewsListResponse>> getTrendingNews(
            @RequestParam(defaultValue = "10") int limit) {
        List<NewsListResponse> response = newsService.getTrendingNews(limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Get related news articles")
    public ResponseEntity<List<NewsListResponse>> getRelatedNews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {
        List<NewsListResponse> response = newsService.getRelatedNews(id, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search news articles")
    public ResponseEntity<PageResponse<NewsListResponse>> searchNews(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<NewsListResponse> response = newsService.searchNews(q, page, size);
        return ResponseEntity.ok(response);
    }

    // ==================== LIKE ENDPOINTS ====================

    @PostMapping("/{id}/like")
    @Operation(summary = "Like a news article")
    public ResponseEntity<MessageResponse> likeNews(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        articleLikeService.likeNews(id, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("News liked"));
    }

    @DeleteMapping("/{id}/like")
    @Operation(summary = "Unlike a news article")
    public ResponseEntity<MessageResponse> unlikeNews(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        articleLikeService.unlikeNews(id, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Like removed"));
    }

    @GetMapping("/{id}/like-status")
    @Operation(summary = "Get like status and count for a news article")
    public ResponseEntity<Map<String, Object>> getNewsLikeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        long count = articleLikeService.getNewsLikeCount(id);
        boolean liked = userDetails != null && articleLikeService.isNewsLiked(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("count", count, "liked", liked));
    }
}
