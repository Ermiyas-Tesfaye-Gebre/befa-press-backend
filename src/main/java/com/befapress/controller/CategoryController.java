package com.befapress.controller;

import com.befapress.dto.response.CategoryResponse;
import com.befapress.dto.response.NewsListResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.CategoryService;
import com.befapress.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category APIs")
public class CategoryController {

    private final CategoryService categoryService;
    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> response = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse response = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/news")
    @Operation(summary = "Get news by category")
    public ResponseEntity<PageResponse<NewsListResponse>> getNewsByCategory(
            @PathVariable String slug,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<NewsListResponse> response = newsService.getNewsByCategory(slug, page, size);
        return ResponseEntity.ok(response);
    }
}
