package com.befapress.service;

import com.befapress.dto.dashboard.DashboardStatsDTO;
import com.befapress.entity.ActivityLog;
import com.befapress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final PageHitRepository pageHitRepository;
    private final CommentRepository commentRepository;
    private final OpinionRepository opinionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CommentReportRepository reportRepository; // Correct repository

    public DashboardStatsDTO getDashboardStats() {
        // 1. Total Counts
        long totalNews = newsRepository.countByDeletedAtIsNull();
        long totalUsers = userRepository.countByDeletedAtIsNull();
        long activeAds = adRepository.count();

        // Date range for views (current month)
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        long totalPageViews = pageHitRepository.countByCreatedAtBetween(startOfMonth, LocalDateTime.now());

        // 2. Pending Counts
        long pendingNews = newsRepository.countByStatusAndDeletedAtIsNull("PENDING");
        long pendingOpinions = opinionRepository.countByStatusAndDeletedAtIsNull("PENDING");
        long flaggedComments = reportRepository.countByStatus("PENDING");

        // 3. Hourly Traffic (Last 24h)
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        List<Object[]> hourlyData = pageHitRepository.getHourlyTraffic(last24h);
        List<Integer> trafficData = processHourlyTraffic(hourlyData);

        return DashboardStatsDTO.builder()
                .totalNews(totalNews)
                .totalUsers(totalUsers)
                .activeAds(activeAds)
                .totalPageViews(totalPageViews)
                // Placeholder changes - in prod calculate vs last month
                .newsChange("+12%")
                .usersChange("+8%")
                .adsChange("-2%")
                .viewsChange("+18%")
                .pendingNews(pendingNews)
                .pendingOpinions(pendingOpinions)
                .flaggedComments(flaggedComments)
                .trafficData(trafficData)
                .build();
    }

    public List<ActivityLog> getRecentActivity() {
        // Use default pageable for limit (e.g. top 10)
        return activityLogRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 10));
    }

    private List<Integer> processHourlyTraffic(List<Object[]> data) {
        // Initialize 24 hours with 0
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < 24; i++)
            map.put(i, 0);

        for (Object[] row : data) {
            int hour = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();
            map.put(hour, count);
        }

        // Return ordered list
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            result.add(map.get(i));
        }
        return result;
    }
}
