package com.befapress.service;

import com.befapress.dto.ModerationResult;
import com.befapress.dto.request.CreateCommentRequest;
import com.befapress.dto.response.CommentResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.entity.Comment;
import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.entity.User;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.CommentRepository;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import com.befapress.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final NewsRepository newsRepository;
    private final OpinionRepository opinionRepository;
    private final UserRepository userRepository;
    private final ContentModerationService moderationService;
    private final SettingsService settingsService;

    // Store likes in-memory (in production, use a separate table)
    private final Map<Long, Set<String>> commentLikes = new HashMap<>();

    // ==================== NEWS COMMENTS (Auto-moderated) ====================

    public PageResponse<CommentResponse> getCommentsByNewsId(Long newsId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByNewsIdAndParentIsNullAndStatus(
                newsId, "APPROVED", pageable);
        return mapToPageResponse(commentPage, true);
    }

    /**
     * Create a news comment with auto-moderation.
     * - Harmful content is rejected immediately (not saved)
     * - Clean content is auto-approved
     */
    @Transactional
    public CommentResponse createComment(Long newsId, CreateCommentRequest request,
            String userEmail, HttpServletRequest httpRequest) {

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        // CHECK SETTINGS
        if (!settingsService.getSettings().getCommentsEnabled()) {
            throw new BadRequestException("Comments are currently disabled.");
        }

        // AUTO-MODERATION: Check content for harmful material
        ModerationResult modResult = moderationService.analyzeContent(request.getContent());

        if (modResult.isHarmful()) {
            // NEWS: Reject immediately, don't save
            log.info("News comment rejected - Categories: {}, Content preview: {}",
                    modResult.getCategoriesAsString(),
                    request.getContent().substring(0, Math.min(50, request.getContent().length())));

            throw new BadRequestException(
                    "Your comment could not be posted. Reason: " + modResult.getReason());
        }

        // Content passed moderation - create and auto-approve
        Comment comment = buildNewsComment(request, userEmail, httpRequest);
        comment.setNews(news);
        comment.setStatus("APPROVED"); // Auto-approve clean content

        comment = commentRepository.save(comment);
        log.info("News comment approved and saved - ID: {}", comment.getId());

        return mapToCommentResponse(comment, false);
    }

    public long getCommentCountByNewsId(Long newsId) {
        return commentRepository.countByNewsIdAndStatus(newsId, "APPROVED");
    }

    // ==================== OPINION COMMENTS (Admin-moderated with flags)
    // ====================

    public PageResponse<CommentResponse> getCommentsByOpinionId(Long opinionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByOpinionIdAndParentIsNullAndStatus(
                opinionId, "APPROVED", pageable);
        return mapToPageResponse(commentPage, true);
    }

    /**
     * Create an opinion comment with AI-assisted flagging for admin review.
     * - All comments are saved (PENDING status)
     * - Harmful content is flagged with detected categories for admin
     */
    @Transactional
    public CommentResponse createOpinionComment(Long opinionId, CreateCommentRequest request,
            String userEmail, HttpServletRequest httpRequest) {

        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

        // CHECK SETTINGS
        if (!settingsService.getSettings().getCommentsEnabled()) {
            throw new BadRequestException("Comments are currently disabled.");
        }

        // AI-ASSISTED FLAGGING: Analyze content and flag for admin
        ModerationResult modResult = moderationService.analyzeContent(request.getContent());

        Comment comment = buildOpinionComment(request, userEmail, httpRequest);
        comment.setOpinion(opinion);

        // Set moderation flags if any detected (for admin review)
        if (modResult.isHarmful()) {
            String flags = String.join(",", modResult.getDetectedCategories());
            comment.setModerationFlags(flags);
            log.info("Opinion comment flagged - Categories: {}", flags);
        }

        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment, false);
    }

    // ==================== REPLIES ====================

    public List<CommentResponse> getReplies(Long commentId) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        return parent.getReplies().stream()
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .map(r -> mapToCommentResponse(r, false))
                .collect(Collectors.toList());
    }

    // ==================== LIKE/UNLIKE ====================

    @Transactional
    public void likeComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        commentLikes.computeIfAbsent(commentId, k -> new HashSet<>()).add(userEmail);
        comment.setLikeCount(commentLikes.get(commentId).size());
        commentRepository.save(comment);
    }

    @Transactional
    public void unlikeComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Set<String> likes = commentLikes.get(commentId);
        if (likes != null) {
            likes.remove(userEmail);
            comment.setLikeCount(likes.size());
            commentRepository.save(comment);
        }
    }

    public int getLikeCount(Long commentId) {
        Set<String> likes = commentLikes.get(commentId);
        return likes != null ? likes.size() : 0;
    }

    // ==================== REPORT ====================

    @Transactional
    public void reportComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        comment.setReported(true);
        comment.setReportCount(comment.getReportCount() + 1);

        if (comment.getReportCount() >= 3) {
            comment.setStatus("HIDDEN");
        }

        commentRepository.save(comment);
    }

    // ==================== DELETE OWN COMMENT ====================

    @Transactional
    public void deleteOwnComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (comment.getUser() == null || !comment.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    // ==================== RECENT COMMENTS ====================

    public List<CommentResponse> getRecentComments(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByStatus("APPROVED", pageable);
        return comments.getContent().stream()
                .map(c -> mapToCommentResponse(c, false))
                .collect(Collectors.toList());
    }

    // ==================== ADMIN METHODS ====================

    public PageResponse<CommentResponse> getPendingComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByStatus("PENDING", pageable);
        return mapToPageResponse(commentPage, false);
    }

    public PageResponse<CommentResponse> getReportedComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByIsReportedTrue(pageable);
        return mapToPageResponse(commentPage, false);
    }

    @Transactional
    public CommentResponse approveComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        comment.setStatus("APPROVED");
        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment, false);
    }

    @Transactional
    public CommentResponse rejectComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        comment.setStatus("REJECTED");
        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment, false);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        commentRepository.delete(comment);
    }

    // ==================== HELPER METHODS ====================

    private Comment buildNewsComment(CreateCommentRequest request, String userEmail, HttpServletRequest httpRequest) {
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setIpAddress(getClientIp(httpRequest));

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
            comment.setParent(parent);
        }

        if (userEmail != null && !userEmail.isEmpty()) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
            comment.setUser(user);
        } else {
            if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
                throw new BadRequestException("Guest name is required");
            }
            comment.setGuestName(request.getGuestName());
            comment.setGuestEmail(request.getGuestEmail());
        }

        return comment;
    }

    private Comment buildOpinionComment(CreateCommentRequest request, String userEmail,
            HttpServletRequest httpRequest) {
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setIpAddress(getClientIp(httpRequest));
        comment.setStatus("PENDING"); // Opinion comments always need admin review

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
            comment.setParent(parent);
        }

        if (userEmail != null && !userEmail.isEmpty()) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
            comment.setUser(user);
        } else {
            if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
                throw new BadRequestException("Guest name is required");
            }
            comment.setGuestName(request.getGuestName());
            comment.setGuestEmail(request.getGuestEmail());
        }

        return comment;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] parts = xForwardedFor.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private PageResponse<CommentResponse> mapToPageResponse(Page<Comment> commentPage, boolean includeReplies) {
        List<CommentResponse> content = commentPage.getContent().stream()
                .map(comment -> mapToCommentResponse(comment, includeReplies))
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(content)
                .page(commentPage.getNumber())
                .size(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .first(commentPage.isFirst())
                .last(commentPage.isLast())
                .build();
    }

    private CommentResponse mapToCommentResponse(Comment comment, boolean includeReplies) {
        List<CommentResponse> replies = Collections.emptyList();
        if (includeReplies && comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replies = comment.getReplies().stream()
                    .filter(r -> "APPROVED".equals(r.getStatus()))
                    .map(r -> mapToCommentResponse(r, false))
                    .collect(Collectors.toList());
        }

        List<String> flags = Collections.emptyList();
        if (comment.getModerationFlags() != null && !comment.getModerationFlags().isEmpty()) {
            flags = Arrays.asList(comment.getModerationFlags().split(","));
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthorName())
                .authorProfilePic(comment.getUser() != null ? comment.getUser().getProfilePic() : null)
                .isGuest(comment.isGuestComment())
                .status(comment.getStatus())
                .likeCount(comment.getLikeCount())
                .moderationFlags(flags)
                .createdAt(comment.getCreatedAt())
                .replies(replies)
                .newsId(comment.getNews() != null ? comment.getNews().getId() : null)
                .newsTitle(comment.getNews() != null ? comment.getNews().getTitle() : null)
                .opinionId(comment.getOpinion() != null ? comment.getOpinion().getId() : null)
                .opinionTitle(comment.getOpinion() != null ? comment.getOpinion().getTitle() : null)
                .build();
    }
}
