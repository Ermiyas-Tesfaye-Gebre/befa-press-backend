package com.befapress.service;

import com.befapress.dto.request.ReportCommentRequest;
import com.befapress.dto.response.PageResponse;
import com.befapress.dto.response.ReportResponse;
import com.befapress.entity.Comment;
import com.befapress.entity.CommentReport;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.CommentReportRepository;
import com.befapress.repository.CommentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;

    /**
     * Create a new report for a comment
     */
    @Transactional
    public ReportResponse createReport(Long commentId, ReportCommentRequest request,
            String reporterEmail, HttpServletRequest httpRequest) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        CommentReport report = CommentReport.builder()
                .comment(comment)
                .reporterEmail(reporterEmail)
                .reporterIp(getClientIp(httpRequest))
                .reason(request.getReason().toUpperCase())
                .description(request.getDescription())
                .status("PENDING")
                .build();

        report = reportRepository.save(report);

        // Also update the comment's reported flag
        comment.setReported(true);
        comment.setReportCount(comment.getReportCount() + 1);
        commentRepository.save(comment);

        log.info("Report created for comment {} with reason: {}", commentId, request.getReason());

        return mapToResponse(report);
    }

    /**
     * Get all pending reports for admin
     */
    public PageResponse<ReportResponse> getPendingReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentReport> reportPage = reportRepository.findByStatus("PENDING", pageable);
        return mapToPageResponse(reportPage);
    }

    /**
     * Get all reports (any status)
     */
    public PageResponse<ReportResponse> getAllReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentReport> reportPage = reportRepository.findAll(pageable);
        return mapToPageResponse(reportPage);
    }

    /**
     * Resolve a report - approve (keep comment) or reject (remove comment)
     */
    @Transactional
    public ReportResponse resolveReport(Long reportId, String resolution, String adminEmail) {
        CommentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        Comment comment = report.getComment();

        if ("APPROVE".equalsIgnoreCase(resolution)) {
            // Keep the comment, mark report as resolved
            report.setStatus("RESOLVED_APPROVED");
            log.info("Report {} resolved: comment approved (kept)", reportId);
        } else if ("REJECT".equalsIgnoreCase(resolution)) {
            // Reject the comment
            comment.setStatus("REJECTED");
            commentRepository.save(comment);
            report.setStatus("RESOLVED_REJECTED");
            log.info("Report {} resolved: comment rejected", reportId);
        }

        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(adminEmail);
        report = reportRepository.save(report);

        return mapToResponse(report);
    }

    /**
     * Get count of pending reports
     */
    public long getPendingReportCount() {
        return reportRepository.countByStatus("PENDING");
    }

    // === Helper Methods ===

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private PageResponse<ReportResponse> mapToPageResponse(Page<CommentReport> reportPage) {
        List<ReportResponse> content = reportPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ReportResponse>builder()
                .content(content)
                .page(reportPage.getNumber())
                .size(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .first(reportPage.isFirst())
                .last(reportPage.isLast())
                .build();
    }

    private ReportResponse mapToResponse(CommentReport report) {
        Comment comment = report.getComment();

        return ReportResponse.builder()
                .id(report.getId())
                .commentId(comment.getId())
                .commentContent(comment.getContent())
                .commentAuthor(comment.getAuthorName())
                .opinionId(comment.getOpinion() != null ? comment.getOpinion().getId() : null)
                .opinionTitle(comment.getOpinion() != null ? comment.getOpinion().getTitle() : null)
                .newsId(comment.getNews() != null ? comment.getNews().getId() : null)
                .newsTitle(comment.getNews() != null ? comment.getNews().getTitle() : null)
                .reporterEmail(report.getReporterEmail())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .resolvedBy(report.getResolvedBy())
                .build();
    }
}
