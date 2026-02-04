package com.befapress.service;

import com.befapress.dto.analytics.LiveAnalyticsDTO;
import com.befapress.repository.PageHitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that broadcasts live analytics updates via WebSocket
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveAnalyticsService {

    private final SimpMessagingTemplate messagingTemplate;
    private final PageHitRepository pageHitRepository;

    /**
     * Broadcast live analytics every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void broadcastLiveAnalytics() {
        try {
            LiveAnalyticsDTO liveData = buildLiveAnalytics();
            messagingTemplate.convertAndSend("/topic/analytics/live", liveData);
            log.debug("Broadcasted live analytics: {} active users", liveData.getActiveUsersNow());
        } catch (Exception e) {
            log.error("Failed to broadcast live analytics", e);
        }
    }

    private LiveAnalyticsDTO buildLiveAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);

        // Active users (sessions in last 5 minutes)
        Long activeUsers = pageHitRepository.countActiveUsers(fiveMinutesAgo);

        // Page views in last minute
        Long viewsLastMinute = pageHitRepository.countByCreatedAtBetween(oneMinuteAgo, now);

        // Real-time device breakdown
        Map<String, Long> deviceBreakdown = new LinkedHashMap<>();
        List<Object[]> devices = pageHitRepository.getDeviceBreakdown(fiveMinutesAgo);
        for (Object[] row : devices) {
            String device = row[0] != null ? (String) row[0] : "Unknown";
            deviceBreakdown.put(device, ((Number) row[1]).longValue());
        }

        return LiveAnalyticsDTO.builder()
                .activeUsersNow(activeUsers != null ? activeUsers : 0L)
                .pageViewsLastMinute(viewsLastMinute != null ? viewsLastMinute : 0L)
                .realtimeDevices(deviceBreakdown)
                .timestamp(now)
                .build();
    }

    /**
     * Force broadcast (can be called manually or from controller)
     */
    public LiveAnalyticsDTO getLiveAnalytics() {
        return buildLiveAnalytics();
    }
}
