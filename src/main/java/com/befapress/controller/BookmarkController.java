package com.befapress.controller;

import com.befapress.dto.response.BookmarkResponse;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.BookmarkService;
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
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Bookmark APIs for saving news and opinions")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // ==================== NEWS BOOKMARKS ====================

    @PostMapping("/news/{newsId}")
    @Operation(summary = "Bookmark a news article")
    public ResponseEntity<BookmarkResponse> bookmarkNews(
            @PathVariable Long newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        BookmarkResponse response = bookmarkService.bookmarkNews(newsId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/news/{newsId}")
    @Operation(summary = "Remove news bookmark")
    public ResponseEntity<MessageResponse> unbookmarkNews(
            @PathVariable Long newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        bookmarkService.unbookmarkNews(newsId, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Bookmark removed"));
    }

    @GetMapping("/news/{newsId}/check")
    @Operation(summary = "Check if news is bookmarked")
    public ResponseEntity<Map<String, Boolean>> checkNewsBookmark(
            @PathVariable Long newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("bookmarked", false));
        }
        boolean bookmarked = bookmarkService.isNewsBookmarked(newsId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    // ==================== OPINION BOOKMARKS ====================

    @PostMapping("/opinion/{opinionId}")
    @Operation(summary = "Bookmark an opinion")
    public ResponseEntity<BookmarkResponse> bookmarkOpinion(
            @PathVariable Long opinionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        BookmarkResponse response = bookmarkService.bookmarkOpinion(opinionId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/opinion/{opinionId}")
    @Operation(summary = "Remove opinion bookmark")
    public ResponseEntity<MessageResponse> unbookmarkOpinion(
            @PathVariable Long opinionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Login required"));
        }
        bookmarkService.unbookmarkOpinion(opinionId, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Bookmark removed"));
    }

    @GetMapping("/opinion/{opinionId}/check")
    @Operation(summary = "Check if opinion is bookmarked")
    public ResponseEntity<Map<String, Boolean>> checkOpinionBookmark(
            @PathVariable Long opinionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("bookmarked", false));
        }
        boolean bookmarked = bookmarkService.isOpinionBookmarked(opinionId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    // ==================== GET MY BOOKMARKS ====================

    @GetMapping
    @Operation(summary = "Get all my bookmarks")
    public ResponseEntity<PageResponse<BookmarkResponse>> getMyBookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        PageResponse<BookmarkResponse> response = bookmarkService.getMyBookmarks(
                userDetails.getUsername(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/news")
    @Operation(summary = "Get my news bookmarks only")
    public ResponseEntity<PageResponse<BookmarkResponse>> getMyNewsBookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        PageResponse<BookmarkResponse> response = bookmarkService.getMyNewsBookmarks(
                userDetails.getUsername(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/opinions")
    @Operation(summary = "Get my opinion bookmarks only")
    public ResponseEntity<PageResponse<BookmarkResponse>> getMyOpinionBookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        PageResponse<BookmarkResponse> response = bookmarkService.getMyOpinionBookmarks(
                userDetails.getUsername(), page, size);
        return ResponseEntity.ok(response);
    }

    // ==================== BULK CHECK ====================

    @GetMapping("/news/ids")
    @Operation(summary = "Get IDs of all bookmarked news articles")
    public ResponseEntity<List<Long>> getBookmarkedNewsIds(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Long> ids = bookmarkService.getBookmarkedNewsIds(userDetails.getUsername());
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/opinions/ids")
    @Operation(summary = "Get IDs of all bookmarked opinions")
    public ResponseEntity<List<Long>> getBookmarkedOpinionIds(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Long> ids = bookmarkService.getBookmarkedOpinionIds(userDetails.getUsername());
        return ResponseEntity.ok(ids);
    }
}
