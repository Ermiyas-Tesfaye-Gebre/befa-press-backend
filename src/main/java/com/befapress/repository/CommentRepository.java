package com.befapress.repository;

import com.befapress.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // News comments
    Page<Comment> findByNewsIdAndParentIsNullAndStatus(Long newsId, String status, Pageable pageable);

    long countByNewsIdAndStatus(Long newsId, String status);

    // Opinion comments
    Page<Comment> findByOpinionIdAndParentIsNullAndStatus(Long opinionId, String status, Pageable pageable);

    long countByOpinionIdAndStatus(Long opinionId, String status);

    // Common
    List<Comment> findByParentId(Long parentId);

    Page<Comment> findByStatus(String status, Pageable pageable);

    Page<Comment> findByIsReportedTrue(Pageable pageable);

    // ========== Analytics Methods ==========

    Long countByNewsId(Long newsId);

    Long countByOpinionId(Long opinionId);
}
