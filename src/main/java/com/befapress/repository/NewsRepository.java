package com.befapress.repository;

import com.befapress.entity.News;
import com.befapress.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Optional<News> findBySlug(String slug);

    Page<News> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<News> findByDeletedAtIsNull(Pageable pageable);

    Page<News> findByCategorySlugAndStatusAndDeletedAtIsNull(String categorySlug, String status, Pageable pageable);

    Page<News> findByAuthorIdAndDeletedAtIsNull(Long authorId, Pageable pageable);

    List<News> findByIsBreakingTrueAndStatusAndDeletedAtIsNull(String status);

    List<News> findByIsFeaturedTrueAndStatusAndDeletedAtIsNull(String status);

    @Query("SELECT n FROM News n WHERE n.status = :status AND n.deletedAt IS NULL ORDER BY n.viewCount DESC")
    List<News> findTrendingNews(@Param("status") String status, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.id != :newsId AND n.category.id = :categoryId AND n.status = 'PUBLISHED' AND n.deletedAt IS NULL ORDER BY n.publishedAt DESC")
    List<News> findRelatedNews(@Param("newsId") Long newsId, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.deletedAt IS NULL AND n.status = 'PUBLISHED' AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<News> searchNews(@Param("search") String search, Pageable pageable);

    boolean existsBySlug(String slug);

    List<News> findTop5ByStatusOrderByPublishedAtDesc(String status);

    boolean existsByCategory(Category category);

    // ========== Analytics Methods ==========

    Long countByAuthorId(Long authorId);

    Long countByCategoryId(Long categoryId);

    long countByStatusAndDeletedAtIsNull(String status);

    long countByDeletedAtIsNull();

    // Count news by status created after a date
    @Query("SELECT COUNT(n) FROM News n WHERE n.status = :status AND n.createdAt >= :since AND n.deletedAt IS NULL")
    long countByStatusAndCreatedAtAfter(@Param("status") String status, @Param("since") java.time.LocalDateTime since);

    // Count news published in period
    @Query("SELECT COUNT(n) FROM News n WHERE n.status = 'PUBLISHED' AND n.publishedAt >= :since AND n.deletedAt IS NULL")
    long countPublishedAfter(@Param("since") java.time.LocalDateTime since);

    // Count by category with grouping
    @Query("SELECT n.category.id, n.category.name, COUNT(n) FROM News n WHERE n.deletedAt IS NULL AND n.status = 'PUBLISHED' GROUP BY n.category.id, n.category.name")
    List<Object[]> countByCategory();

    // Count by language
    @Query("SELECT n.language, COUNT(n) FROM News n WHERE n.deletedAt IS NULL AND n.status = 'PUBLISHED' GROUP BY n.language")
    List<Object[]> countByLanguage();

    // Get top news by views in period
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' AND n.deletedAt IS NULL ORDER BY n.viewCount DESC")
    List<News> findTopByViews(Pageable pageable);
}
