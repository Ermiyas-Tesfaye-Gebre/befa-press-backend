package com.befapress.repository;

import com.befapress.entity.Subscription;
import com.befapress.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Subscription entity
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByEndDateBetween(LocalDateTime start, LocalDateTime end);

    List<Subscription> findByEndDateBeforeAndStatus(LocalDateTime date, SubscriptionStatus status);

    List<Subscription> findByStatusAndLastPromotionalEmailBeforeOrLastPromotionalEmailIsNull(
            SubscriptionStatus status, LocalDateTime date);
}
