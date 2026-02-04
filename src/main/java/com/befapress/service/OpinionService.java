package com.befapress.service;

import com.befapress.dto.request.CreateOpinionRequest;
import com.befapress.dto.response.*;
import com.befapress.entity.Opinion;
import com.befapress.entity.User;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.OpinionRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class OpinionService {

    private final OpinionRepository opinionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final com.befapress.service.social.SocialShareService socialShareService;
    private final ActivityLogService activityLogService;

    public PageResponse<OpinionResponse> getAllPublishedOpinions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Opinion> opinionPage = opinionRepository.findByStatusAndDeletedAtIsNull("PUBLISHED", pageable);
        return mapToPageResponse(opinionPage);
    }

    public OpinionResponse getOpinionBySlug(String slug) {
        Opinion opinion = opinionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "slug", slug));

        if (!"PUBLISHED".equals(opinion.getStatus()) || opinion.isDeleted()) {
            throw new ResourceNotFoundException("Opinion", "slug", slug);
        }

        return mapToOpinionResponse(opinion);
    }

    public OpinionResponse getOpinionById(Long id) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));
        return mapToOpinionResponse(opinion);
    }

    @Transactional
    public void incrementViewCount(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));
        opinion.incrementViewCount();
        opinionRepository.save(opinion);
    }

    public List<OpinionResponse> getFeaturedOpinions() {
        List<Opinion> featuredOpinions = opinionRepository.findByIsFeaturedTrueAndStatusAndDeletedAtIsNull("PUBLISHED");
        return featuredOpinions.stream()
                .map(this::mapToOpinionResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<OpinionResponse> getOpinionsByAuthor(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Opinion> opinionPage = opinionRepository.findByAuthorIdAndDeletedAtIsNull(authorId, pageable);
        return mapToPageResponse(opinionPage);
    }

    public PageResponse<OpinionResponse> searchOpinions(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Opinion> opinionPage = opinionRepository.searchOpinions(query, pageable);
        return mapToPageResponse(opinionPage);
    }

    public PageResponse<OpinionResponse> getMyOpinions(String authorEmail, int page, int size) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Opinion> opinionPage = opinionRepository.findByAuthorIdAndDeletedAtIsNull(author.getId(), pageable);
        return mapToPageResponse(opinionPage);
    }

    // Intellectual methods (for opinion writers)
    @Transactional
    public OpinionResponse createOpinion(CreateOpinionRequest request, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        // Intellectuals can only create drafts or submit for pending review
        String status = request.getStatus();
        if (!"DRAFT".equals(status) && !"PENDING".equals(status)) {
            status = "DRAFT";
        }

        String slug = generateSlug(request.getTitle());
        if (opinionRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Opinion opinion = Opinion.builder()
                .title(request.getTitle())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .author(author)
                .status(status)
                .isFeatured(false) // Only admin can set featured
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .scheduledAt(request.getScheduledAt())
                .language(detectLanguage(request.getTitle(), request.getContent()))
                .build();

        opinion = opinionRepository.save(opinion);

        // Log Activity
        if ("PENDING".equals(opinion.getStatus())) {
            activityLogService.logActivity(
                    "OPINION",
                    "New opinion submitted for review: " + opinion.getTitle(),
                    author.getFullName(),
                    opinion.getId());
        } else {
            activityLogService.logActivity(
                    "OPINION",
                    "New opinion draft created: " + opinion.getTitle(),
                    author.getFullName(),
                    opinion.getId());
        }

        return mapToOpinionResponse(opinion);
    }

    @Transactional
    public OpinionResponse updateOpinion(Long id, CreateOpinionRequest request, String authorEmail) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        // Verify ownership
        if (!opinion.getAuthor().getEmail().equals(authorEmail)) {
            throw new BadRequestException("You can only edit your own opinions");
        }

        // Can only edit if not published
        if ("PUBLISHED".equals(opinion.getStatus())) {
            throw new BadRequestException("Cannot edit published opinions. Please contact admin.");
        }

        opinion.setTitle(request.getTitle());
        opinion.setExcerpt(request.getExcerpt());
        opinion.setContent(request.getContent());
        opinion.setCoverImage(request.getCoverImage());
        opinion.setMetaTitle(request.getMetaTitle());
        opinion.setMetaDescription(request.getMetaDescription());
        opinion.setScheduledAt(request.getScheduledAt());
        opinion.setLanguage(detectLanguage(request.getTitle(), request.getContent()));

        // Can submit for review
        if ("PENDING".equals(request.getStatus())) {
            boolean wasDraft = "DRAFT".equals(opinion.getStatus());
            opinion.setStatus("PENDING");

            if (wasDraft) {
                activityLogService.logActivity(
                        "OPINION",
                        "Opinion submitted for review: " + opinion.getTitle(),
                        opinion.getAuthor().getFullName(),
                        opinion.getId());
            }
        }

        opinion = opinionRepository.save(opinion);
        return mapToOpinionResponse(opinion);
    }

    @Transactional
    public OpinionResponse submitOpinion(Long id, String authorEmail) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        // Verify ownership
        if (!opinion.getAuthor().getEmail().equals(authorEmail)) {
            throw new BadRequestException("You can only submit your own opinions");
        }

        // Can only submit if DRAFT or REJECTED (?)
        // Assuming we allow re-submission
        opinion.setStatus("PENDING");
        opinion = opinionRepository.save(opinion);

        activityLogService.logActivity(
                "OPINION",
                "Opinion submitted for review: " + opinion.getTitle(),
                opinion.getAuthor().getFullName(),
                opinion.getId());

        return mapToOpinionResponse(opinion);

    }

    @Transactional
    public void deleteOwnOpinion(Long id, String authorEmail) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        // Verify ownership
        if (!opinion.getAuthor().getEmail().equals(authorEmail)) {
            throw new BadRequestException("You can only delete your own opinions");
        }

        // Verify status (Can only delete DRAFT or REJECTED)
        if (!"DRAFT".equals(opinion.getStatus()) && !"REJECTED".equals(opinion.getStatus())) {
            throw new BadRequestException(
                    "You can only delete DRAFT or REJECTED opinions. 'PENDING' or 'PUBLISHED' opinions cannot be deleted.");
        }

        opinion.setDeletedAt(LocalDateTime.now());
        opinionRepository.save(opinion);
    }

    // Admin methods
    @Transactional
    public OpinionResponse approveOpinion(Long id) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        opinion.setStatus("PUBLISHED");
        opinion.setPublishedAt(LocalDateTime.now());
        opinion = opinionRepository.save(opinion);

        activityLogService.logActivity(
                "OPINION",
                "Opinion approved and published: " + opinion.getTitle(),
                "System", // Or admin name if available but context is tricky here
                opinion.getId());

        // Send notification email
        emailService.sendOpinionApprovalEmail(
                opinion.getAuthor().getEmail(),
                opinion.getAuthor().getFullName(),
                opinion.getTitle(),
                true);

        // Trigger social media sharing
        socialShareService.shareOpinion(opinion);

        return mapToOpinionResponse(opinion);
    }

    @Transactional
    public OpinionResponse rejectOpinion(Long id, String reason) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        opinion.setStatus("REJECTED");
        opinion.setRejectionReason(reason);
        opinion = opinionRepository.save(opinion);

        activityLogService.logActivity(
                "OPINION",
                "Opinion rejected: " + opinion.getTitle(),
                "System",
                opinion.getId());

        // Send notification email
        emailService.sendOpinionApprovalEmail(
                opinion.getAuthor().getEmail(),
                opinion.getAuthor().getFullName(),
                opinion.getTitle(),
                false);

        return mapToOpinionResponse(opinion);
    }

    @Transactional
    public OpinionResponse setFeatured(Long id, boolean featured) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        opinion.setFeatured(featured);
        opinion = opinionRepository.save(opinion);
        return mapToOpinionResponse(opinion);
    }

    @Transactional
    public void deleteOpinion(Long id) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));
        opinion.setDeletedAt(LocalDateTime.now());
        opinionRepository.save(opinion);
    }

    public PageResponse<OpinionResponse> getAdminOpinionsList(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Opinion> opinionPage;

        if (status != null && !status.isEmpty()) {
            opinionPage = opinionRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else {
            opinionPage = opinionRepository.findByDeletedAtIsNull(pageable);
        }

        return mapToPageResponse(opinionPage);
    }

    // Helper methods
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private PageResponse<OpinionResponse> mapToPageResponse(Page<Opinion> opinionPage) {
        List<OpinionResponse> content = opinionPage.getContent().stream()
                .map(this::mapToOpinionResponse)
                .collect(Collectors.toList());

        return PageResponse.<OpinionResponse>builder()
                .content(content)
                .page(opinionPage.getNumber())
                .size(opinionPage.getSize())
                .totalElements(opinionPage.getTotalElements())
                .totalPages(opinionPage.getTotalPages())
                .first(opinionPage.isFirst())
                .last(opinionPage.isLast())
                .build();
    }

    private OpinionResponse mapToOpinionResponse(Opinion opinion) {
        return OpinionResponse.builder()
                .id(opinion.getId())
                .title(opinion.getTitle())
                .slug(opinion.getSlug())
                .excerpt(opinion.getExcerpt())
                .content(opinion.getContent())
                .coverImage(opinion.getCoverImage())
                .author(mapToAuthorResponse(opinion.getAuthor()))
                .status(opinion.getStatus())
                .viewCount(opinion.getViewCount())
                .isFeatured(opinion.isFeatured())
                .metaTitle(opinion.getMetaTitle())
                .metaDescription(opinion.getMetaDescription())
                .scheduledAt(opinion.getScheduledAt())
                .publishedAt(opinion.getPublishedAt())
                .createdAt(opinion.getCreatedAt())
                .updatedAt(opinion.getUpdatedAt())
                .rejectionReason(opinion.getRejectionReason())
                .build();
    }

    private AuthorResponse mapToAuthorResponse(User user) {
        return AuthorResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .profilePic(user.getProfilePic())
                .affiliation(user.getAffiliation())
                .expertiseField(user.getExpertiseField())
                .isVerified(user.isVerified())
                .build();
    }

    private String detectLanguage(String title, String content) {
        String combined = (title != null ? title : "") + " " + (content != null ? content : "");
        // Check for Ethiopic characters range \u1200-\u137F
        if (combined.matches(".*[\\u1200-\\u137F]+.*")) {
            return "am";
        }
        return "en";
    }
}
