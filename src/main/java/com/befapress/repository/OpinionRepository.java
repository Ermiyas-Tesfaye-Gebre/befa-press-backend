package com.befapress.repository;

import com.befapress.entity.Opinion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    Optional<Opinion> findBySlug(String slug);

    Page<Opinion> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<Opinion> findByAuthorIdAndDeletedAtIsNull(Long authorId, Pageable pageable);

    List<Opinion> findByIsFeaturedTrueAndStatusAndDeletedAtIsNull(String status);

    boolean existsBySlug(String slug);

    long countByStatusAndDeletedAtIsNull(String status);

    Page<Opinion> findByDeletedAtIsNull(Pageable pageable);

    // Search opinions by title or content
    @Query("SELECT o FROM Opinion o WHERE o.deletedAt IS NULL AND o.status = 'PUBLISHED' AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Opinion> searchOpinions(@Param("search") String search, Pageable pageable);

    // ========== Analytics Methods ==========

    // countByStatusAndDeletedAtIsNull removed (duplicate)

    Long countByAuthorId(Long authorId);

    // Count opinions by author with grouping
    @Query("SELECT o.author.id, o.author.fullName, COUNT(o) FROM Opinion o WHERE o.deletedAt IS NULL AND o.status = 'PUBLISHED' GROUP BY o.author.id, o.author.fullName ORDER BY COUNT(o) DESC")
    List<Object[]> countByAuthorGrouped(Pageable pageable);

    // Count opinions published in period
    @Query("SELECT COUNT(o) FROM Opinion o WHERE o.status = 'PUBLISHED' AND o.publishedAt >= :since AND o.deletedAt IS NULL")
    long countPublishedAfter(@Param("since") java.time.LocalDateTime since);

    // Count by language
    @Query("SELECT o.language, COUNT(o) FROM Opinion o WHERE o.deletedAt IS NULL AND o.status = 'PUBLISHED' GROUP BY o.language")
    List<Object[]> countByLanguage();
}
