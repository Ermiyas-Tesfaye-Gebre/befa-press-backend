package com.befapress.service;

import com.befapress.entity.ActivityLog;
import com.befapress.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(String type, String message, String actor, Long relatedId) {
        try {
            ActivityLog log = ActivityLog.builder()
                    .type(type)
                    .message(message)
                    .actor(actor)
                    .relatedId(relatedId)
                    .build();
            log = activityLogRepository.save(log);

            // Broadcast to WebSocket topic
            messagingTemplate.convertAndSend("/topic/activities", log);

        } catch (Exception e) {
            // Activity logging should not fail the main transaction
            System.err.println("Failed to log activity: " + e.getMessage());
        }
    }
}
