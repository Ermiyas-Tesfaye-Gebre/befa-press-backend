package com.befapress.repository;

import com.befapress.entity.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Advertisement, Long> {

    // Simple finder, date filtering moved to service
    List<Advertisement> findByPlacementZoneAndAdStatus(String placementZone, String adStatus);

    // Find ads by status
    Page<Advertisement> findByAdStatus(String adStatus, Pageable pageable);

    // Find ads expiring before a certain date (for cron)
    List<Advertisement> findByAdStatusAndEndDateBefore(String adStatus, LocalDateTime date);

    Long countByAdStatus(String status);

    // Aggregate stats by placement zone for analytics
    @Query("SELECT a.placementZone, SUM(a.views), SUM(a.clicks) " +
            "FROM Advertisement a " +
            "GROUP BY a.placementZone")
    List<Object[]> getPerformanceByZone();

    // Get total impressions and clicks across all ads
    @Query("SELECT COALESCE(SUM(a.views), 0), COALESCE(SUM(a.clicks), 0) FROM Advertisement a")
    Object[] getTotalImpressionAndClicks();
}
