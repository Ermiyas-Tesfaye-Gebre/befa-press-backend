package com.befapress.repository;

import com.befapress.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Get latest activities
    List<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
