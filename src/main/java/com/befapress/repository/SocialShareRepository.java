package com.befapress.repository;

import com.befapress.entity.SocialShare;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SocialShareRepository extends JpaRepository<SocialShare, Long> {

    // Count shares for an article
    Long countByEntityTypeAndEntityId(String entityType, Long entityId);

    // Platform breakdown
    @Query("SELECT ss.platform, COUNT(ss) FROM SocialShare ss " +
            "WHERE ss.createdAt >= :since GROUP BY ss.platform ORDER BY COUNT(ss) DESC")
    List<Object[]> getPlatformBreakdown(@Param("since") LocalDateTime since);

    // Top shared articles
    @Query("SELECT ss.entityId, ss.entityType, COUNT(ss) as shareCount FROM SocialShare ss " +
            "WHERE ss.createdAt >= :since " +
            "GROUP BY ss.entityId, ss.entityType ORDER BY shareCount DESC")
    List<Object[]> getTopSharedArticles(@Param("since") LocalDateTime since, Pageable pageable);

    // Total shares in period
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Shares for specific article
    @Query("SELECT ss.platform, COUNT(ss) FROM SocialShare ss " +
            "WHERE ss.entityType = :type AND ss.entityId = :id GROUP BY ss.platform")
    List<Object[]> getSharesByArticle(@Param("type") String type, @Param("id") Long id);
}
