package com.befapress.service;

import com.befapress.entity.Notification;
import com.befapress.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void createNotification(String message, Notification.NotificationType type, Long relatedId) {
        Notification notification = Notification.builder()
                .message(message)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }
}
