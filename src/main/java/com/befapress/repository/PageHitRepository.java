package com.befapress.repository;

import com.befapress.entity.PageHit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageHitRepository extends JpaRepository<PageHit, Long> {

        // ========== View Counts ==========

        Long countByEntityTypeAndEntityId(String entityType, Long entityId);

        Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        @Query("SELECT COUNT(DISTINCT ph.sessionId) FROM PageHit ph WHERE ph.createdAt BETWEEN :start AND :end")
        Long countUniqueSessionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // ========== Top Articles ==========

        @Query("SELECT ph.entityId, COUNT(ph) as cnt FROM PageHit ph " +
                        "WHERE ph.entityType = :type AND ph.createdAt >= :since " +
                        "GROUP BY ph.entityId ORDER BY cnt DESC")
        List<Object[]> findTopByEntityType(@Param("type") String type,
                        @Param("since") LocalDateTime since,
                        Pageable pageable);

        @Query("SELECT ph.entityId, COUNT(ph) as views, AVG(ph.timeOnPage) as avgTime, " +
                        "AVG(ph.scrollDepth) as avgScroll " +
                        "FROM PageHit ph WHERE ph.entityType = :type AND ph.createdAt >= :since " +
                        "GROUP BY ph.entityId ORDER BY views DESC")
        List<Object[]> findTopWithEngagement(@Param("type") String type,
                        @Param("since") LocalDateTime since,
                        Pageable pageable);

        // ========== Trending (Recent 24h) ==========

        @Query("SELECT ph.entityId, ph.entityType, COUNT(ph) as cnt FROM PageHit ph " +
                        "WHERE ph.createdAt >= :since " +
                        "GROUP BY ph.entityId, ph.entityType ORDER BY cnt DESC")
        List<Object[]> findTrending(@Param("since") LocalDateTime since, Pageable pageable);

        // ========== Device Breakdown ==========

        @Query("SELECT ph.device, COUNT(ph) FROM PageHit ph " +
                        "WHERE ph.createdAt >= :since GROUP BY ph.device")
        List<Object[]> getDeviceBreakdown(@Param("since") LocalDateTime since);

        // ========== Geographic Distribution ==========

        @Query("SELECT ph.country, COUNT(ph) FROM PageHit ph " +
                        "WHERE ph.createdAt >= :since AND ph.country IS NOT NULL " +
                        "GROUP BY ph.country ORDER BY COUNT(ph) DESC")
        List<Object[]> getCountryBreakdown(@Param("since") LocalDateTime since);

        // ========== Language Preference ==========

        @Query("SELECT ph.language, COUNT(ph) FROM PageHit ph " +
                        "WHERE ph.createdAt >= :since AND ph.language IS NOT NULL " +
                        "GROUP BY ph.language")
        List<Object[]> getLanguageBreakdown(@Param("since") LocalDateTime since);

        // ========== Traffic Trends ==========

        @Query("SELECT FUNCTION('DATE', ph.createdAt) as date, COUNT(ph) as views, " +
                        "COUNT(DISTINCT ph.sessionId) as uniqueUsers " +
                        "FROM PageHit ph WHERE ph.createdAt BETWEEN :start AND :end " +
                        "GROUP BY FUNCTION('DATE', ph.createdAt) ORDER BY date")
        List<Object[]> getDailyTraffic(@Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT FUNCTION('MONTH', ph.createdAt) as month, COUNT(ph) as views, " +
                        "COUNT(DISTINCT ph.sessionId) as uniqueUsers " +
                        "FROM PageHit ph WHERE FUNCTION('YEAR', ph.createdAt) = :year " +
                        "GROUP BY FUNCTION('MONTH', ph.createdAt) ORDER BY month")
        List<Object[]> getMonthlyTraffic(@Param("year") int year);

        @Query("SELECT FUNCTION('HOUR', ph.createdAt) as hour, COUNT(ph) as views " +
                        "FROM PageHit ph WHERE ph.createdAt >= :since " +
                        "GROUP BY FUNCTION('HOUR', ph.createdAt) ORDER BY hour")
        List<Object[]> getHourlyTraffic(@Param("since") LocalDateTime since);

        // ========== Referrer Sources ==========

        @Query("SELECT CASE " +
                        "WHEN ph.referrer LIKE '%google%' THEN 'Google' " +
                        "WHEN ph.referrer LIKE '%facebook%' THEN 'Facebook' " +
                        "WHEN ph.referrer LIKE '%twitter%' OR ph.referrer LIKE '%x.com%' THEN 'Twitter/X' " +
                        "WHEN ph.referrer LIKE '%telegram%' THEN 'Telegram' " +
                        "WHEN ph.referrer IS NULL OR ph.referrer = '' THEN 'Direct' " +
                        "ELSE 'Other' END as source, COUNT(ph) " +
                        "FROM PageHit ph WHERE ph.createdAt >= :since GROUP BY source")
        List<Object[]> getReferrerSources(@Param("since") LocalDateTime since);

        // ========== Bounce Rate ==========

        @Query("SELECT COUNT(ph) FROM PageHit ph WHERE ph.isBounce = true AND ph.createdAt >= :since")
        Long countBounces(@Param("since") LocalDateTime since);

        // ========== Average Session Duration ==========

        @Query("SELECT AVG(ph.timeOnPage) FROM PageHit ph WHERE ph.createdAt >= :since")
        Double getAverageTimeOnPage(@Param("since") LocalDateTime since);

        // ========== Real-time (Last 5 minutes) ==========

        @Query("SELECT COUNT(DISTINCT ph.sessionId) FROM PageHit ph WHERE ph.createdAt >= :since")
        Long countActiveUsers(@Param("since") LocalDateTime since);

        // ========== Article Stats ==========

        @Query("SELECT COUNT(ph), AVG(ph.timeOnPage), AVG(ph.scrollDepth), " +
                        "SUM(CASE WHEN ph.isBounce = true THEN 1 ELSE 0 END) " +
                        "FROM PageHit ph WHERE ph.entityType = :type AND ph.entityId = :id")
        Object[] getArticleStats(@Param("type") String type, @Param("id") Long id);

        // ========== Author Performance ==========

        @Query(value = "SELECT n.author_id, COUNT(ph.hit_id) as views " +
                        "FROM page_hits ph " +
                        "INNER JOIN news n ON ph.entity_id = n.news_id AND ph.entity_type = 'NEWS' " +
                        "WHERE ph.created_at >= :since " +
                        "GROUP BY n.author_id ORDER BY views DESC", nativeQuery = true)
        List<Object[]> getTopAuthorsByViews(@Param("since") LocalDateTime since, Pageable pageable);
}
