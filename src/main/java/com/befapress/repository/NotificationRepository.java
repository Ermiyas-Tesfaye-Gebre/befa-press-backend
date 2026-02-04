package com.befapress.repository;

import com.befapress.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find all unread notifications ordered by latest first
    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();

    // Find recent notifications (read or unread)
    List<Notification> findTop20ByOrderByCreatedAtDesc();

    long countByIsReadFalse();
}
