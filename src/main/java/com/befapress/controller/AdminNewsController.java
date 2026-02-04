package com.befapress.controller;

import com.befapress.dto.request.CreateNewsRequest;
import com.befapress.dto.response.*;
import com.befapress.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
@Tag(name = "Admin - News", description = "Admin news management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EDITOR')")
public class AdminNewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "Get all news with optional status filter")
    public ResponseEntity<PageResponse<NewsListResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        PageResponse<NewsListResponse> response = newsService.getAdminNewsList(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get news by ID")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        NewsResponse response = newsService.getNewsById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new news article")
    public ResponseEntity<NewsResponse> createNews(
            @Valid @RequestBody CreateNewsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        NewsResponse response = newsService.createNews(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a news article")
    public ResponseEntity<NewsResponse> updateNews(
            @PathVariable Long id,
            @Valid @RequestBody CreateNewsRequest request) {
        NewsResponse response = newsService.updateNews(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a news article (soft delete)")
    public ResponseEntity<MessageResponse> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok(MessageResponse.success("News deleted successfully"));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish a news article")
    public ResponseEntity<NewsResponse> publishNews(@PathVariable Long id) {
        NewsResponse response = newsService.publishNews(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/archive")
    @Operation(summary = "Archive a news article")
    public ResponseEntity<NewsResponse> archiveNews(@PathVariable Long id) {
        NewsResponse response = newsService.archiveNews(id);
        return ResponseEntity.ok(response);
    }
}
