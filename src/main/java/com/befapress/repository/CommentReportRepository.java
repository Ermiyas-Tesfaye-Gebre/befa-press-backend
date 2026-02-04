package com.befapress.repository;

import com.befapress.entity.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    Page<CommentReport> findByStatus(String status, Pageable pageable);

    List<CommentReport> findByCommentId(Long commentId);

    long countByStatus(String status);
}
