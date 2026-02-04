package com.befapress.service;

import com.befapress.dto.request.CreateCategoryRequest;
import com.befapress.dto.response.CategoryResponse;
import com.befapress.entity.Category;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final com.befapress.repository.NewsRepository newsRepository;

    public List<CategoryResponse> getAllActiveCategories() {
        List<Category> categories = categoryRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE");
        return categories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return mapToCategoryResponse(category);
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToCategoryResponse(category);
    }

    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with this name already exists");
        }

        String slug = generateSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Category category = Category.builder()
                .name(request.getName())
                .nameAmharic(request.getNameAmharic())
                .slug(slug)
                .description(request.getDescription())
                .icon(request.getIcon())
                .displayOrder(request.getDisplayOrder())
                .status(request.getStatus())
                .build();

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setName(request.getName());
        category.setNameAmharic(request.getNameAmharic());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setStatus(request.getStatus());

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (newsRepository.existsByCategory(category)) {
            throw new BadRequestException("Cannot delete category. It is being used by news articles.");
        }

        categoryRepository.delete(category);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .nameAmharic(category.getNameAmharic())
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .status(category.getStatus())
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}
