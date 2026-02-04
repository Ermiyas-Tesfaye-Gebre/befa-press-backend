package com.befapress.repository;

import com.befapress.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<LoginHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<LoginHistory> findByUserIdAndStatusAndCreatedAtAfter(Long userId, String status, LocalDateTime after);

    long countByUserIdAndStatusAndCreatedAtAfter(Long userId, String status, LocalDateTime after);
}
