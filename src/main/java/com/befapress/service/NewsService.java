package com.befapress.service;

import com.befapress.dto.request.CreateNewsRequest;
import com.befapress.dto.response.*;
import com.befapress.entity.Category;
import com.befapress.entity.News;
import com.befapress.entity.User;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.CategoryRepository;
import com.befapress.repository.CommentRepository;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final com.befapress.service.social.SocialShareService socialShareService;
    private final ActivityLogService activityLogService;

    public PageResponse<NewsListResponse> getAllPublishedNews(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<News> newsPage = newsRepository.findByStatusAndDeletedAtIsNull("PUBLISHED", pageable);
        return mapToPageResponse(newsPage);
    }

    public PageResponse<NewsListResponse> getNewsByCategory(String categorySlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<News> newsPage = newsRepository.findByCategorySlugAndStatusAndDeletedAtIsNull(
                categorySlug, "PUBLISHED", pageable);
        return mapToPageResponse(newsPage);
    }

    public NewsResponse getNewsBySlug(String slug) {
        News news = newsRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("News", "slug", slug));

        if (!"PUBLISHED".equals(news.getStatus()) || news.isDeleted()) {
            throw new ResourceNotFoundException("News", "slug", slug);
        }

        return mapToNewsResponse(news);
    }

    public NewsResponse getNewsById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));
        return mapToNewsResponse(news);
    }

    @Transactional
    public void incrementViewCount(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));
        news.incrementViewCount();
        newsRepository.save(news);
    }

    public List<NewsListResponse> getBreakingNews(int limit) {
        List<News> breakingNews = newsRepository.findByIsBreakingTrueAndStatusAndDeletedAtIsNull("PUBLISHED");
        return breakingNews.stream()
                .limit(limit)
                .map(this::mapToNewsListResponse)
                .collect(Collectors.toList());
    }

    public List<NewsListResponse> getFeaturedNews() {
        List<News> featuredNews = newsRepository.findByIsFeaturedTrueAndStatusAndDeletedAtIsNull("PUBLISHED");
        return featuredNews.stream()
                .map(this::mapToNewsListResponse)
                .collect(Collectors.toList());
    }

    public List<NewsListResponse> getTrendingNews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<News> trendingNews = newsRepository.findTrendingNews("PUBLISHED", pageable);
        return trendingNews.stream()
                .map(this::mapToNewsListResponse)
                .collect(Collectors.toList());
    }

    public List<NewsListResponse> getRelatedNews(Long newsId, int limit) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        Pageable pageable = PageRequest.of(0, limit);
        List<News> relatedNews = newsRepository.findRelatedNews(newsId, news.getCategory().getId(), pageable);
        return relatedNews.stream()
                .map(this::mapToNewsListResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<NewsListResponse> searchNews(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<News> newsPage = newsRepository.searchNews(query, pageable);
        return mapToPageResponse(newsPage);
    }

    // Admin methods
    @Transactional
    public NewsResponse createNews(CreateNewsRequest request, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        String slug = generateSlug(request.getTitle());
        if (newsRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        News news = News.builder()
                .title(request.getTitle())
                .titleAmharic(request.getTitleAmharic())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .author(author)
                .category(category)
                .status(request.getStatus())
                .isFeatured(request.isFeatured())
                .isBreaking(request.isBreaking())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .scheduledAt(request.getScheduledAt())
                .language(detectLanguage(request.getTitle(), request.getContent()))
                .build();

        if ("PUBLISHED".equals(request.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }

        news = newsRepository.save(news);

        // Log Activity
        activityLogService.logActivity(
                "NEWS",
                "New news article created: " + news.getTitle(),
                author.getFullName(),
                news.getId());

        // Trigger social media sharing if published immediately
        if ("PUBLISHED".equals(news.getStatus())) {
            socialShareService.shareNews(news);
            activityLogService.logActivity(
                    "NEWS",
                    "News article published: " + news.getTitle(),
                    author.getFullName(),
                    news.getId());
        }

        return mapToNewsResponse(news);
    }

    @Transactional
    public NewsResponse updateNews(Long id, CreateNewsRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));
        String oldStatus = news.getStatus();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            news.setCategory(category);
        }

        news.setTitle(request.getTitle());
        news.setTitleAmharic(request.getTitleAmharic());
        news.setExcerpt(request.getExcerpt());
        news.setContent(request.getContent());
        news.setCoverImage(request.getCoverImage());
        news.setFeatured(request.isFeatured());
        news.setBreaking(request.isBreaking());
        news.setMetaTitle(request.getMetaTitle());
        news.setMetaDescription(request.getMetaDescription());
        news.setMetaKeywords(request.getMetaKeywords());
        news.setScheduledAt(request.getScheduledAt());
        news.setLanguage(detectLanguage(request.getTitle(), request.getContent()));

        // Handle status change
        if ("PUBLISHED".equals(request.getStatus()) && !"PUBLISHED".equals(news.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }
        news.setStatus(request.getStatus());

        news = newsRepository.save(news);

        // Trigger social media sharing if status changed to PUBLISHED
        if ("PUBLISHED".equals(request.getStatus()) && !"PUBLISHED".equals(oldStatus)) {
            socialShareService.shareNews(news);
        } else if ("PUBLISHED".equals(request.getStatus()) && "PUBLISHED".equals(oldStatus)) {
            // Already published, maybe updated content?
            // Usually we don't re-share updates unless explicitly requested.
            // For now, only share on first publish.
        }

        return mapToNewsResponse(news);
    }

    @Transactional
    public void deleteNews(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));
        news.setDeletedAt(LocalDateTime.now());
        newsRepository.save(news);
    }

    @Transactional
    public NewsResponse publishNews(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));

        news.setStatus("PUBLISHED");
        news.setPublishedAt(LocalDateTime.now());

        news = newsRepository.save(news);

        // Trigger social media sharing
        socialShareService.shareNews(news);

        return mapToNewsResponse(news);
    }

    public PageResponse<NewsListResponse> getAdminNewsList(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<News> newsPage;

        if (status != null && !status.isEmpty()) {
            newsPage = newsRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else {
            // Return all non-deleted news regardless of status
            newsPage = newsRepository.findByDeletedAtIsNull(pageable);
        }

        return mapToPageResponse(newsPage);
    }

    @Transactional
    public NewsResponse archiveNews(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));

        news.setStatus("ARCHIVED");
        news = newsRepository.save(news);
        return mapToNewsResponse(news);
    }

    // Helper methods
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private PageResponse<NewsListResponse> mapToPageResponse(Page<News> newsPage) {
        List<NewsListResponse> content = newsPage.getContent().stream()
                .map(this::mapToNewsListResponse)
                .collect(Collectors.toList());

        return PageResponse.<NewsListResponse>builder()
                .content(content)
                .page(newsPage.getNumber())
                .size(newsPage.getSize())
                .totalElements(newsPage.getTotalElements())
                .totalPages(newsPage.getTotalPages())
                .first(newsPage.isFirst())
                .last(newsPage.isLast())
                .build();
    }

    private NewsListResponse mapToNewsListResponse(News news) {
        return NewsListResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .titleAmharic(news.getTitleAmharic())
                .slug(news.getSlug())
                .excerpt(news.getExcerpt())
                .coverImage(news.getCoverImage())
                .authorName(news.getAuthor().getFullName())
                .categoryName(news.getCategory().getName())
                .categorySlug(news.getCategory().getSlug())
                .categoryId(news.getCategory().getId())
                .status(news.getStatus())
                .viewCount(news.getViewCount())
                .isFeatured(news.isFeatured())
                .isBreaking(news.isBreaking())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .build();
    }

    private NewsResponse mapToNewsResponse(News news) {
        long commentCount = commentRepository.countByNewsIdAndStatus(news.getId(), "APPROVED");

        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .titleAmharic(news.getTitleAmharic())
                .slug(news.getSlug())
                .excerpt(news.getExcerpt())
                .content(news.getContent())
                .coverImage(news.getCoverImage())
                .author(mapToAuthorResponse(news.getAuthor()))
                .category(mapToCategoryResponse(news.getCategory()))
                .status(news.getStatus())
                .viewCount(news.getViewCount())
                .isFeatured(news.isFeatured())
                .isBreaking(news.isBreaking())
                .isTrending(news.isTrending())
                .metaTitle(news.getMetaTitle())
                .metaDescription(news.getMetaDescription())
                .metaKeywords(news.getMetaKeywords())
                .scheduledAt(news.getScheduledAt())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .commentCount(commentCount)
                .build();
    }

    private AuthorResponse mapToAuthorResponse(User user) {
        return AuthorResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .profilePic(user.getProfilePic())
                .affiliation(user.getAffiliation())
                .expertiseField(user.getExpertiseField())
                .isVerified(user.isVerified())
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .nameAmharic(category.getNameAmharic())
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .build();
    }

    private String detectLanguage(String title, String content) {
        String combined = (title != null ? title : "") + " " + (content != null ? content : "");
        // Check for Ethiopic characters range \u1200-\u137F
        if (combined.matches(".*[\\u1200-\\u137F]+.*")) {
            return "am";
        }
        return "en";
    }
}
