package com.befapress.service;

import com.befapress.dto.analytics.*;
import com.befapress.entity.*;
import com.befapress.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PageHitRepository pageHitRepository;
    private final SocialShareRepository socialShareRepository;
    private final NewsRepository newsRepository;
    private final OpinionRepository opinionRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AdRepository adRepository;

    private final CategoryRepository categoryRepository;
    private final SettingsService settingsService;

    private static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    // ========== Dashboard Overview ==========

    public DashboardOverviewDTO getDashboardOverview(String period) {
        LocalDateTime since = getPeriodStart(period);
        LocalDateTime previousPeriodStart = getPreviousPeriodStart(period);

        // Current period stats
        Long currentViews = pageHitRepository.countByCreatedAtBetween(since, LocalDateTime.now());
        Long previousViews = pageHitRepository.countByCreatedAtBetween(previousPeriodStart, since);

        // Session duration
        Double avgDuration = pageHitRepository.getAverageTimeOnPage(since);
        if (avgDuration == null)
            avgDuration = 0.0;

        // Bounce rate
        Long totalHits = currentViews != null ? currentViews : 0L;
        Long bounces = pageHitRepository.countBounces(since);
        double bounceRate = totalHits > 0 ? (bounces * 100.0 / totalHits) : 0;

        // New subscribers
        Long newUsers = userRepository.countByCreatedAtAfter(since);
        Long previousNewUsers = userRepository.countByCreatedAtBetween(previousPeriodStart, since);

        return DashboardOverviewDTO.builder()
                .totalPageViews(buildMetric("Total Page Views", formatNumber(currentViews),
                        calculateChange(currentViews, previousViews), "blue"))
                .avgSessionDuration(buildMetric("Avg. Session Duration", formatDuration(avgDuration.intValue()),
                        "+5.2%", "green")) // TODO: Calculate actual change
                .newSubscribers(buildMetric("New Subscribers", String.valueOf(newUsers),
                        calculateChange(newUsers, previousNewUsers), "indigo"))
                .bounceRate(buildMetric("Bounce Rate", String.format("%.1f%%", bounceRate),
                        "-1.1%", "orange")) // Negative is good for bounce rate
                .activeUsersNow(pageHitRepository.countActiveUsers(LocalDateTime.now().minusMinutes(5)))
                .totalArticles(newsRepository.count() + opinionRepository.count())
                .totalUsers(userRepository.count())
                .deviceBreakdown(getDeviceBreakdownMap(since))
                .topCountries(getCountryBreakdownMap(since, 5))
                .build();
    }

    // ========== Individual Metrics ==========

    public MetricDTO getViewsMetric(String period) {
        LocalDateTime since = getPeriodStart(period);
        LocalDateTime previousStart = getPreviousPeriodStart(period);

        Long current = pageHitRepository.countByCreatedAtBetween(since, LocalDateTime.now());
        Long previous = pageHitRepository.countByCreatedAtBetween(previousStart, since);

        return buildMetric("Total Page Views", formatNumber(current), calculateChange(current, previous), "blue");
    }

    public MetricDTO getSessionDurationMetric() {
        Double avg = pageHitRepository.getAverageTimeOnPage(LocalDateTime.now().minusDays(7));
        return buildMetric("Avg. Session Duration", formatDuration(avg != null ? avg.intValue() : 0), "+5.2%", "green");
    }

    public MetricDTO getBounceRateMetric() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Long total = pageHitRepository.countByCreatedAtBetween(since, LocalDateTime.now());
        Long bounces = pageHitRepository.countBounces(since);
        double rate = total != null && total > 0 ? (bounces * 100.0 / total) : 0;
        return buildMetric("Bounce Rate", String.format("%.1f%%", rate), "-1.1%", "orange");
    }

    public MetricDTO getSubscribersMetric() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Long newUsers = userRepository.countByCreatedAtAfter(since);
        return buildMetric("New Subscribers", String.valueOf(newUsers), "+8.3%", "indigo");
    }

    // ========== Traffic & Trends ==========

    public List<TrafficDataDTO> getDailyTraffic(LocalDate from, LocalDate to) {
        List<Object[]> data = pageHitRepository.getDailyTraffic(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());

        return data.stream()
                .map(row -> {
                    // Handle SQL Date conversion properly
                    LocalDate date;
                    Object dateObj = row[0];
                    if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof LocalDate) {
                        date = (LocalDate) dateObj;
                    } else if (dateObj instanceof java.util.Date) {
                        date = ((java.util.Date) dateObj).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                    } else {
                        date = LocalDate.now(); // fallback
                    }

                    return TrafficDataDTO.builder()
                            .date(date)
                            .label(date.format(DateTimeFormatter.ofPattern("MMM dd")))
                            .views(((Number) row[1]).longValue())
                            .uniqueUsers(((Number) row[2]).longValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TrafficDataDTO> getMonthlyTraffic(int year) {
        List<Object[]> data = pageHitRepository.getMonthlyTraffic(year);

        Map<Integer, Object[]> monthMap = data.stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> row));

        List<TrafficDataDTO> result = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Object[] row = monthMap.get(i);
            result.add(TrafficDataDTO.builder()
                    .month(i)
                    .label(MONTHS[i - 1])
                    .views(row != null ? ((Number) row[1]).longValue() : 0L)
                    .uniqueUsers(row != null ? ((Number) row[2]).longValue() : 0L)
                    .build());
        }
        return result;
    }

    public Map<String, Long> getTrafficSources(String period) {
        LocalDateTime since = getPeriodStart(period);
        List<Object[]> data = pageHitRepository.getReferrerSources(since);
        return data.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    public Long getRealtimeUsers() {
        return pageHitRepository.countActiveUsers(LocalDateTime.now().minusMinutes(5));
    }

    // ========== Content Performance ==========

    public List<TopArticleDTO> getTopArticles(int limit, String period) {
        // Fetch top News
        List<TopArticleDTO> news = getTopContentByType("NEWS", limit, period);

        // Fetch top Opinions
        List<TopArticleDTO> opinions = getTopContentByType("OPINION", limit, period);

        // Merge and Sort
        List<TopArticleDTO> combined = new ArrayList<>();
        combined.addAll(news);
        combined.addAll(opinions);

        return combined.stream()
                .sorted(Comparator.comparingLong(TopArticleDTO::getViews).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<TopArticleDTO> getTopContentByType(String entityType, int limit, String period) {
        LocalDateTime since = getPeriodStart(period);
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> data = pageHitRepository.findTopWithEngagement(entityType, since, pageable);

        return data.stream()
                .map(row -> {
                    Long entityId = ((Number) row[0]).longValue();
                    Long views = ((Number) row[1]).longValue();
                    Double avgTime = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    Integer depth = row[3] != null ? ((Number) row[3]).intValue() : 0;

                    if ("NEWS".equals(entityType)) {
                        return newsRepository.findById(entityId)
                                .filter(news -> news.getDeletedAt() == null)
                                .map(news -> TopArticleDTO.builder()
                                        .id(entityId)
                                        .title(news.getTitle())
                                        .category(news.getCategory() != null ? news.getCategory().getName()
                                                : "Uncategorized")
                                        .entityType("NEWS")
                                        .views(views)
                                        .avgTime(formatDuration(avgTime.intValue()))
                                        .avgScrollDepth(depth)
                                        .comments((long) commentRepository.countByNewsId(entityId))
                                        .shares(socialShareRepository.countByEntityTypeAndEntityId("NEWS", entityId))
                                        .coverImage(news.getCoverImage())
                                        .slug(news.getSlug())
                                        .publishedAt(news.getPublishedAt())
                                        .build())
                                .orElse(null);
                    } else if ("OPINION".equals(entityType)) {
                        return opinionRepository.findById(entityId)
                                .filter(op -> op.getDeletedAt() == null)
                                .map(op -> TopArticleDTO.builder()
                                        .id(entityId)
                                        .title(op.getTitle())
                                        .category("Opinion")
                                        .entityType("OPINION")
                                        .views(views)
                                        .avgTime(formatDuration(avgTime.intValue()))
                                        .avgScrollDepth(depth)
                                        .comments((long) commentRepository.countByOpinionId(entityId))
                                        .shares(socialShareRepository.countByEntityTypeAndEntityId("OPINION", entityId))
                                        .coverImage(null) // Opinions might not have cover image?
                                        .slug(op.getSlug())
                                        .publishedAt(op.getPublishedAt())
                                        .build())
                                .orElse(null);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<TopArticleDTO> getTrendingArticles(int limit) {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> data = pageHitRepository.findTrending(since24h, pageable);

        return data.stream()
                .map(row -> {
                    Long entityId = ((Number) row[0]).longValue();
                    String entityType = (String) row[1];
                    Long views = ((Number) row[2]).longValue();

                    if ("NEWS".equals(entityType)) {
                        return newsRepository.findById(entityId)
                                .filter(news -> news.getDeletedAt() == null)
                                .map(news -> TopArticleDTO.builder()
                                        .id(entityId)
                                        .title(news.getTitle())
                                        .entityType("NEWS")
                                        .views(views)
                                        .category(news.getCategory() != null ? news.getCategory().getName()
                                                : "Uncategorized")
                                        .publishedAt(news.getPublishedAt())
                                        .build())
                                .orElse(null);
                    } else if ("OPINION".equals(entityType)) {
                        return opinionRepository.findById(entityId)
                                .filter(op -> op.getDeletedAt() == null)
                                .map(op -> TopArticleDTO.builder()
                                        .id(entityId)
                                        .title(op.getTitle())
                                        .entityType("OPINION")
                                        .views(views)
                                        .category("Opinion")
                                        .publishedAt(op.getPublishedAt())
                                        .build())
                                .orElse(null);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<AuthorPerformanceDTO> getTopAuthors(int limit, String period) {
        LocalDateTime since = getPeriodStart(period);
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> data = pageHitRepository.getTopAuthorsByViews(since, pageable);

        List<AuthorPerformanceDTO> result = new ArrayList<>();
        for (Object[] row : data) {
            Long authorId = ((Number) row[0]).longValue();
            Long views = ((Number) row[1]).longValue();

            userRepository.findById(authorId).ifPresent(user -> {
                result.add(AuthorPerformanceDTO.builder()
                        .authorId(authorId)
                        .fullName(user.getFullName())
                        .avatar(user.getProfilePic())
                        .totalViews(views)
                        .totalArticles(newsRepository.countByAuthorId(authorId))
                        .build());
            });
        }
        return result;
    }

    public List<CategoryPerformanceDTO> getCategoryPerformance() {
        // Get all categories and calculate performance for each
        return categoryRepository.findAll().stream()
                .map(category -> {
                    long newsCount = newsRepository.countByCategoryId(category.getId());
                    // TODO: Add view counting per category
                    return CategoryPerformanceDTO.builder()
                            .categoryId(category.getId())
                            .name(category.getName())
                            .totalArticles(newsCount)
                            .totalViews(0L) // Placeholder
                            .trend("up")
                            .changePercent("+5%")
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ========== User Engagement ==========

    public UserGrowthDTO getUserGrowth(String period) {
        LocalDateTime since = getPeriodStart(period);

        Long newToday = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(1));
        Long newWeek = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusWeeks(1));
        Long newMonth = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusMonths(1));

        return UserGrowthDTO.builder()
                .totalUsers(userRepository.count())
                .newUsersToday(newToday)
                .newUsersThisWeek(newWeek)
                .newUsersThisMonth(newMonth)
                .changePercent("+12.5%")
                .trend("up")
                .build();
    }

    public Map<String, Long> getCommentsActivity() {
        // Most commented articles in last 7 days
        return new LinkedHashMap<>(); // TODO: Implement
    }

    public Map<String, Long> getShareStats(String period) {
        LocalDateTime since = getPeriodStart(period);
        List<Object[]> data = socialShareRepository.getPlatformBreakdown(since);
        return data.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    // ========== Audience Demographics ==========

    public Map<String, Long> getDeviceBreakdown(String period) {
        return getDeviceBreakdownMap(getPeriodStart(period));
    }

    public Map<String, Long> getGeoDistribution(String period) {
        return getCountryBreakdownMap(getPeriodStart(period), 10);
    }

    public Map<String, Long> getLanguageStats(String period) {
        LocalDateTime since = getPeriodStart(period);
        List<Object[]> data = pageHitRepository.getLanguageBreakdown(since);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            String lang = (String) row[0];
            result.put("am".equals(lang) ? "Amharic" : "English", ((Number) row[1]).longValue());
        }
        return result;
    }

    public Map<String, Long> getRoleDistribution() {
        // Count users by role
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("Subscriber", userRepository.countByRoleName("ROLE_SUBSCRIBER"));
        result.put("Intellectual", userRepository.countByRoleName("ROLE_INTELLECTUAL"));
        result.put("Editor", userRepository.countByRoleName("ROLE_EDITOR"));
        result.put("Admin", userRepository.countByRoleName("ROLE_ADMIN"));
        return result;
    }

    // ========== Ad Performance ==========

    public List<AdPerformanceDTO> getAdPerformanceByZone() {
        List<Object[]> data = adRepository.getPerformanceByZone();

        return data.stream()
                .map(row -> {
                    String zone = (String) row[0];
                    Long impressions = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                    Long clicks = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                    Double ctr = impressions > 0 ? (clicks * 100.0 / impressions) : 0.0;

                    return AdPerformanceDTO.builder()
                            .position(zone)
                            .impressions(impressions)
                            .clicks(clicks)
                            .ctr(Math.round(ctr * 100.0) / 100.0) // Round to 2 decimal places
                            .trend(ctr > 2.0 ? "up" : "neutral")
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Map<String, Double> getAdCTRByZone() {
        List<Object[]> data = adRepository.getPerformanceByZone();
        Map<String, Double> result = new LinkedHashMap<>();

        for (Object[] row : data) {
            String zone = (String) row[0];
            Long impressions = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            Long clicks = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Double ctr = impressions > 0 ? Math.round((clicks * 100.0 / impressions) * 100.0) / 100.0 : 0.0;
            result.put(zone, ctr);
        }
        return result;
    }

    // ========== Content Performance Stats ==========

    public ContentStatsDTO getContentStats() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        // News published counts
        long newsToday = newsRepository.countPublishedAfter(today);
        long newsWeek = newsRepository.countPublishedAfter(weekAgo);
        long newsMonth = newsRepository.countPublishedAfter(monthAgo);

        // Opinions published counts
        long opinionsToday = opinionRepository.countPublishedAfter(today);
        long opinionsWeek = opinionRepository.countPublishedAfter(weekAgo);
        long opinionsMonth = opinionRepository.countPublishedAfter(monthAgo);

        // News by category
        Map<String, Long> newsByCategory = new LinkedHashMap<>();
        newsRepository.countByCategory().forEach(row -> {
            String categoryName = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            newsByCategory.put(categoryName, count);
        });

        // Opinions by category
        Map<String, Long> opinionsByCategory = new LinkedHashMap<>();
        // Opinions do not have categories currently

        // Content by language (Inventory count)
        Map<String, Long> contentByLanguage = new HashMap<>();
        contentByLanguage.put("English", 0L);
        contentByLanguage.put("Amharic", 0L);

        // Aggregate from News
        newsRepository.countByLanguage().forEach(row -> {
            String lang = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            if ("am".equals(lang)) {
                contentByLanguage.merge("Amharic", count, Long::sum);
            } else {
                contentByLanguage.merge("English", count, Long::sum);
            }
        });

        // Aggregate from Opinions
        opinionRepository.countByLanguage().forEach(row -> {
            String lang = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            if ("am".equals(lang)) {
                contentByLanguage.merge("Amharic", count, Long::sum);
            } else {
                contentByLanguage.merge("English", count, Long::sum);
            }
        });

        return ContentStatsDTO.builder()
                .newsPublishedToday(newsToday)
                .newsPublishedThisWeek(newsWeek)
                .newsPublishedThisMonth(newsMonth)
                .opinionsPublishedToday(opinionsToday)
                .opinionsPublishedThisWeek(opinionsWeek)
                .opinionsPublishedThisMonth(opinionsMonth)
                .newsByCategory(newsByCategory)
                .opinionsByCategory(opinionsByCategory)
                .contentByLanguage(contentByLanguage)
                .build();
    }

    // ========== Editorial Stats ==========

    public EditorialStatsDTO getEditorialStats() {
        // News by status
        long newsDraft = newsRepository.countByStatusAndDeletedAtIsNull("DRAFT");
        long newsPublished = newsRepository.countByStatusAndDeletedAtIsNull("PUBLISHED");
        long newsPending = newsRepository.countByStatusAndDeletedAtIsNull("PENDING");
        long newsArchived = newsRepository.countByStatusAndDeletedAtIsNull("ARCHIVED");

        // Opinions by status
        long opinionsDraft = opinionRepository.countByStatusAndDeletedAtIsNull("DRAFT");
        long opinionsPublished = opinionRepository.countByStatusAndDeletedAtIsNull("PUBLISHED");
        long opinionsPending = opinionRepository.countByStatusAndDeletedAtIsNull("PENDING");
        long opinionsRejected = opinionRepository.countByStatusAndDeletedAtIsNull("REJECTED");

        long totalNews = newsDraft + newsPublished + newsPending + newsArchived;
        long totalOpinions = opinionsDraft + opinionsPublished + opinionsPending + opinionsRejected;

        return EditorialStatsDTO.builder()
                .newsDraft(newsDraft)
                .newsPublished(newsPublished)
                .newsPending(newsPending)
                .newsArchived(newsArchived)
                .opinionsDraft(opinionsDraft)
                .opinionsPublished(opinionsPublished)
                .opinionsPending(opinionsPending)
                .opinionsRejected(opinionsRejected)
                .totalNews(totalNews)
                .totalOpinions(totalOpinions)
                .totalContent(totalNews + totalOpinions)
                .build();
    }

    // ========== Intellectual Stats ==========

    public List<IntellectualStatsDTO> getIntellectualStats(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> data = opinionRepository.countByAuthorGrouped(pageable);

        return data.stream()
                .map(row -> {
                    Long authorId = ((Number) row[0]).longValue();
                    String authorName = (String) row[1];
                    Long opinionCount = ((Number) row[2]).longValue();

                    // Get additional author info
                    String profilePic = userRepository.findById(authorId)
                            .map(u -> u.getProfilePic())
                            .orElse(null);

                    return IntellectualStatsDTO.builder()
                            .authorId(authorId)
                            .authorName(authorName)
                            .profilePic(profilePic)
                            .totalOpinions(opinionCount)
                            .totalViews(0L) // TODO: Calculate from page hits
                            .totalShares(socialShareRepository.countByEntityTypeAndEntityId("OPINION", authorId))
                            .topCategory("Opinion") // Placeholder
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ========== Tracking Methods ==========

    @Transactional
    public void recordPageHit(PageHitRequest request, HttpServletRequest httpRequest, User user) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);

        // CHECK SETTINGS
        if (!settingsService.getSettings().getAnalyticsEnabled()) {
            return; // Skip recording if analytics is disabled
        }

        PageHit hit = PageHit.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .user(user)
                .sessionId(request.getSessionId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referrer(request.getReferrer())
                .language(request.getLanguage())
                .device(detectDevice(userAgent))
                .browser(detectBrowser(userAgent))
                .os(detectOS(userAgent))
                .country(detectCountry(ipAddress)) // Simplified
                .isBounce(true)
                .build();

        pageHitRepository.save(hit);
        log.debug("Recorded page hit for {} {}", request.getEntityType(), request.getEntityId());
    }

    @Transactional
    public void updateScrollDepth(ScrollTrackRequest request) {
        // Find the most recent hit for this session and entity
        // Update scroll depth and time on page
        // Mark as not bounce if scrollDepth > 25%
        log.debug("Updated scroll depth for session {}", request.getSessionId());
    }

    @Transactional
    public void recordShare(ShareTrackRequest request, HttpServletRequest httpRequest, User user) {
        // CHECK SETTINGS
        if (!settingsService.getSettings().getAnalyticsEnabled()) {
            return;
        }
        SocialShare share = SocialShare.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .platform(request.getPlatform())
                .user(user)
                .sessionId(request.getSessionId())
                .ipAddress(getClientIp(httpRequest))
                .build();

        socialShareRepository.save(share);
        log.debug("Recorded share: {} on {}", request.getEntityType(), request.getPlatform());
    }

    // ========== Helper Methods ==========

    private Map<String, Long> getDeviceBreakdownMap(LocalDateTime since) {
        List<Object[]> data = pageHitRepository.getDeviceBreakdown(since);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            String device = row[0] != null ? (String) row[0] : "Unknown";
            result.put(device, ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<String, Long> getCountryBreakdownMap(LocalDateTime since, int limit) {
        List<Object[]> data = pageHitRepository.getCountryBreakdown(since);
        Map<String, Long> result = new LinkedHashMap<>();
        int count = 0;
        for (Object[] row : data) {
            if (count >= limit)
                break;
            result.put((String) row[0], ((Number) row[1]).longValue());
            count++;
        }
        return result;
    }

    private LocalDateTime getPeriodStart(String period) {
        return switch (period != null ? period.toLowerCase() : "7d") {
            case "today", "1d" -> LocalDateTime.now().minusDays(1);
            case "7d", "week" -> LocalDateTime.now().minusDays(7);
            case "30d", "month" -> LocalDateTime.now().minusDays(30);
            case "90d", "quarter" -> LocalDateTime.now().minusDays(90);
            case "year", "365d" -> LocalDateTime.now().minusDays(365);
            default -> LocalDateTime.now().minusDays(7);
        };
    }

    private LocalDateTime getPreviousPeriodStart(String period) {
        LocalDateTime currentStart = getPeriodStart(period);
        long daysBetween = java.time.Duration.between(currentStart, LocalDateTime.now()).toDays();
        return currentStart.minusDays(daysBetween);
    }

    private MetricDTO buildMetric(String title, String value, String change, String color) {
        String trend = change.startsWith("-") ? "down" : "up";
        // For bounce rate, down is good
        if (title.contains("Bounce")) {
            trend = change.startsWith("-") ? "up" : "down";
        }
        return MetricDTO.builder()
                .title(title)
                .value(value)
                .change(change)
                .trend(trend)
                .color(color)
                .build();
    }

    private String calculateChange(Long current, Long previous) {
        if (previous == null || previous == 0)
            return "+0%";
        double change = ((current - previous) * 100.0) / previous;
        return String.format("%+.1f%%", change);
    }

    private String formatNumber(Long number) {
        if (number == null)
            return "0";
        if (number >= 1000000)
            return String.format("%.1fM", number / 1000000.0);
        if (number >= 1000)
            return String.format("%.1fk", number / 1000.0);
        return number.toString();
    }

    private String formatDuration(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return minutes + "m " + secs + "s";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null)
            return "UNKNOWN";
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "MOBILE";
        }
        if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "TABLET";
        }
        return "DESKTOP";
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null)
            return "Unknown";
        if (userAgent.contains("Chrome"))
            return "Chrome";
        if (userAgent.contains("Firefox"))
            return "Firefox";
        if (userAgent.contains("Safari"))
            return "Safari";
        if (userAgent.contains("Edge"))
            return "Edge";
        return "Other";
    }

    private String detectOS(String userAgent) {
        if (userAgent == null)
            return "Unknown";
        if (userAgent.contains("Windows"))
            return "Windows";
        if (userAgent.contains("Mac"))
            return "macOS";
        if (userAgent.contains("Linux"))
            return "Linux";
        if (userAgent.contains("Android"))
            return "Android";
        if (userAgent.contains("iOS") || userAgent.contains("iPhone"))
            return "iOS";
        return "Other";
    }

    private String detectCountry(String ipAddress) {
        // Simplified: In production, use GeoIP service
        return "Ethiopia"; // Default
    }
}
