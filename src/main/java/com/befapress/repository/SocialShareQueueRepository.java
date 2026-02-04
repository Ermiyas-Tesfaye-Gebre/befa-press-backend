package com.befapress.repository;

import com.befapress.entity.SocialPlatformConfig;
import com.befapress.entity.SocialShareQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SocialShareQueueRepository extends JpaRepository<SocialShareQueue, Long> {

    List<SocialShareQueue> findByStatus(SocialShareQueue.ShareStatus status);

    List<SocialShareQueue> findByStatusAndAttemptsLessThan(
            SocialShareQueue.ShareStatus status, int maxAttempts);

    Page<SocialShareQueue> findByEntityTypeAndEntityId(
            SocialShareQueue.EntityType entityType, Long entityId, Pageable pageable);

    Page<SocialShareQueue> findByPlatform(
            SocialPlatformConfig.Platform platform, Pageable pageable);

    @Query("SELECT q FROM SocialShareQueue q WHERE q.status = 'PENDING' ORDER BY q.createdAt ASC")
    List<SocialShareQueue> findPendingShares();

    @Query("SELECT q FROM SocialShareQueue q WHERE q.status = 'FAILED' AND q.attempts < :maxAttempts")
    List<SocialShareQueue> findRetryableShares(int maxAttempts);

    long countByStatus(SocialShareQueue.ShareStatus status);

    long countByPlatformAndStatus(SocialPlatformConfig.Platform platform, SocialShareQueue.ShareStatus status);

    boolean existsByEntityTypeAndEntityIdAndPlatformAndStatusIn(
            SocialShareQueue.EntityType entityType,
            Long entityId,
            SocialPlatformConfig.Platform platform,
            List<SocialShareQueue.ShareStatus> statuses);
}
