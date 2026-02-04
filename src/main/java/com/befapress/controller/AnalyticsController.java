package com.befapress.controller;

import com.befapress.dto.analytics.*;
import com.befapress.entity.User;
import com.befapress.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics APIs for dashboard metrics and tracking")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ========== Dashboard & Overview ==========

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview with all key metrics")
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getDashboardOverview(period));
    }

    @GetMapping("/metrics/views")
    @Operation(summary = "Get total page views metric")
    public ResponseEntity<MetricDTO> getViewsMetric(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getViewsMetric(period));
    }

    @GetMapping("/metrics/session-duration")
    @Operation(summary = "Get average session duration metric")
    public ResponseEntity<MetricDTO> getSessionDurationMetric() {
        return ResponseEntity.ok(analyticsService.getSessionDurationMetric());
    }

    @GetMapping("/metrics/bounce-rate")
    @Operation(summary = "Get bounce rate metric")
    public ResponseEntity<MetricDTO> getBounceRateMetric() {
        return ResponseEntity.ok(analyticsService.getBounceRateMetric());
    }

    @GetMapping("/metrics/subscribers")
    @Operation(summary = "Get new subscribers metric")
    public ResponseEntity<MetricDTO> getSubscribersMetric() {
        return ResponseEntity.ok(analyticsService.getSubscribersMetric());
    }

    // ========== Traffic & Trends ==========

    @GetMapping("/traffic/daily")
    @Operation(summary = "Get daily traffic data for charts")
    public ResponseEntity<List<TrafficDataDTO>> getDailyTraffic(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();
        return ResponseEntity.ok(analyticsService.getDailyTraffic(from, to));
    }

    @GetMapping("/traffic/monthly")
    @Operation(summary = "Get monthly traffic data for the year")
    public ResponseEntity<List<TrafficDataDTO>> getMonthlyTraffic(
            @RequestParam(required = false) Integer year) {
        if (year == null)
            year = Year.now().getValue();
        return ResponseEntity.ok(analyticsService.getMonthlyTraffic(year));
    }

    @GetMapping("/traffic/sources")
    @Operation(summary = "Get traffic sources breakdown")
    public ResponseEntity<Map<String, Long>> getTrafficSources(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getTrafficSources(period));
    }

    @GetMapping("/traffic/realtime")
    @Operation(summary = "Get active users count (last 5 minutes)")
    public ResponseEntity<Map<String, Object>> getRealtimeUsers() {
        Long count = analyticsService.getRealtimeUsers();
        return ResponseEntity.ok(Map.of("activeUsers", count, "timestamp", System.currentTimeMillis()));
    }

    // ========== Content Performance ==========

    @GetMapping("/top-articles")
    @Operation(summary = "Get top performing articles")
    public ResponseEntity<List<TopArticleDTO>> getTopArticles(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getTopArticles(limit, period));
    }

    @GetMapping("/top-authors")
    @Operation(summary = "Get top authors by views")
    public ResponseEntity<List<AuthorPerformanceDTO>> getTopAuthors(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(analyticsService.getTopAuthors(limit, period));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get category performance breakdown")
    public ResponseEntity<List<CategoryPerformanceDTO>> getCategoryPerformance() {
        return ResponseEntity.ok(analyticsService.getCategoryPerformance());
    }

    @GetMapping("/article/{id}/stats")
    @Operation(summary = "Get detailed stats for a single article")
    public ResponseEntity<TopArticleDTO> getArticleStats(@PathVariable Long id) {
        // TODO: Implement single article stats
        return ResponseEntity.ok(TopArticleDTO.builder().id(id).build());
    }

    @GetMapping("/trending")
    @Operation(summary = "Get currently trending articles (last 24h)")
    public ResponseEntity<List<TopArticleDTO>> getTrending(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.getTrendingArticles(limit));
    }

    // ========== User Engagement ==========

    @GetMapping("/users/growth")
    @Operation(summary = "Get user registration growth data")
    public ResponseEntity<UserGrowthDTO> getUserGrowth(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(analyticsService.getUserGrowth(period));
    }

    @GetMapping("/users/retention")
    @Operation(summary = "Get user retention rate")
    public ResponseEntity<Map<String, Object>> getUserRetention() {
        // TODO: Implement retention calculation
        return ResponseEntity.ok(Map.of("retentionRate", 65.5, "period", "7d"));
    }

    @GetMapping("/comments/activity")
    @Operation(summary = "Get most commented articles")
    public ResponseEntity<Map<String, Long>> getCommentsActivity() {
        return ResponseEntity.ok(analyticsService.getCommentsActivity());
    }

    @GetMapping("/comments/top-users")
    @Operation(summary = "Get most active commenters")
    public ResponseEntity<List<Map<String, Object>>> getTopCommenters() {
        // TODO: Implement top commenters
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/shares")
    @Operation(summary = "Get social share statistics")
    public ResponseEntity<Map<String, Long>> getShareStats(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getShareStats(period));
    }

    // ========== Audience Demographics ==========

    @GetMapping("/audience/devices")
    @Operation(summary = "Get device breakdown (Mobile/Desktop/Tablet)")
    public ResponseEntity<Map<String, Long>> getDeviceBreakdown(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getDeviceBreakdown(period));
    }

    @GetMapping("/audience/geo")
    @Operation(summary = "Get geographic distribution by country")
    public ResponseEntity<Map<String, Long>> getGeoDistribution(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getGeoDistribution(period));
    }

    @GetMapping("/audience/languages")
    @Operation(summary = "Get language preference stats")
    public ResponseEntity<Map<String, Long>> getLanguageStats(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getLanguageStats(period));
    }

    @GetMapping("/audience/roles")
    @Operation(summary = "Get user role distribution")
    public ResponseEntity<Map<String, Long>> getRoleDistribution() {
        return ResponseEntity.ok(analyticsService.getRoleDistribution());
    }

    // ========== Technical & Ads ==========

    @GetMapping("/ads/performance")
    @Operation(summary = "Get ad impressions and clicks by zone")
    public ResponseEntity<List<AdPerformanceDTO>> getAdPerformance() {
        return ResponseEntity.ok(analyticsService.getAdPerformanceByZone());
    }

    // ========== Content Performance Reports ==========

    @GetMapping("/content/stats")
    @Operation(summary = "Get content statistics (news/opinions by period and category)")
    public ResponseEntity<ContentStatsDTO> getContentStats() {
        return ResponseEntity.ok(analyticsService.getContentStats());
    }

    // ========== Editorial Reports ==========

    @GetMapping("/editorial/stats")
    @Operation(summary = "Get editorial statistics (draft/published/rejected/pending counts)")
    public ResponseEntity<EditorialStatsDTO> getEditorialStats() {
        return ResponseEntity.ok(analyticsService.getEditorialStats());
    }

    // ========== Intellectual/Author Reports ==========

    @GetMapping("/editorial/intellectuals")
    @Operation(summary = "Get opinions published per intellectual author")
    public ResponseEntity<List<IntellectualStatsDTO>> getIntellectualStats(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getIntellectualStats(limit));
    }

    @GetMapping("/content/most-shared")
    @Operation(summary = "Get most shared articles")
    public ResponseEntity<Map<String, Long>> getMostSharedArticles(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(analyticsService.getShareStats(period));
    }

    @GetMapping("/ads/ctr")
    @Operation(summary = "Get click-through rate by ad zone")
    public ResponseEntity<Map<String, Double>> getAdCTR() {
        return ResponseEntity.ok(analyticsService.getAdCTRByZone());
    }

    @GetMapping("/technical/page-load")
    @Operation(summary = "Get average page load time")
    public ResponseEntity<Map<String, Object>> getPageLoadTime() {
        // TODO: Implement page load tracking
        return ResponseEntity.ok(Map.of("avgLoadTime", 1.2, "unit", "seconds"));
    }

    @GetMapping("/technical/errors")
    @Operation(summary = "Get 404/500 error statistics")
    public ResponseEntity<Map<String, Long>> getErrorStats() {
        // TODO: Implement error tracking
        return ResponseEntity.ok(Map.of("404", 0L, "500", 0L));
    }

    // ========== Tracking Endpoints (Public) ==========

    @PostMapping("/track")
    @Operation(summary = "Record a page hit")
    public ResponseEntity<Void> trackPageHit(
            @RequestBody PageHitRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal User user) {
        analyticsService.recordPageHit(request, httpRequest, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/track/scroll")
    @Operation(summary = "Update scroll depth for a page view")
    public ResponseEntity<Void> trackScroll(@RequestBody ScrollTrackRequest request) {
        analyticsService.updateScrollDepth(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/track/share")
    @Operation(summary = "Record a social share event")
    public ResponseEntity<Void> trackShare(
            @RequestBody ShareTrackRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal User user) {
        analyticsService.recordShare(request, httpRequest, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/track/session")
    @Operation(summary = "Update session duration")
    public ResponseEntity<Void> trackSession(@RequestBody Map<String, Object> request) {
        // TODO: Implement session tracking
        return ResponseEntity.ok().build();
    }
}
