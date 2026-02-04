package com.befapress.service.social;

import com.befapress.entity.*;
import com.befapress.repository.SocialPlatformConfigRepository;
import com.befapress.repository.SocialShareQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates social media sharing across all platforms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialShareService {

    private final SocialPlatformConfigRepository configRepository;
    private final SocialShareQueueRepository queueRepository;
    private final com.befapress.repository.NewsRepository newsRepository;
    private final com.befapress.repository.OpinionRepository opinionRepository;
    private final TelegramPublisher telegramPublisher;
    private final FacebookPublisher facebookPublisher;

    @Value("${app.base-url:http://localhost:9090}")
    private String baseUrl;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Queue a news article for sharing to all enabled platforms.
     */
    @Async
    @Transactional
    public void shareNews(News news) {
        if (news == null || !"PUBLISHED".equals(news.getStatus())) {
            return;
        }

        List<SocialPlatformConfig> enabledPlatforms = configRepository.findByEnabledTrueAndShareNewsTrue();

        for (SocialPlatformConfig config : enabledPlatforms) {
            queueShare(SocialShareQueue.EntityType.NEWS, news.getId(), config.getPlatform());
        }

        // Process immediately
        processQueue();
    }

    /**
     * Queue an opinion article for sharing to all enabled platforms.
     */
    @Async
    @Transactional
    public void shareOpinion(Opinion opinion) {
        if (opinion == null || !"PUBLISHED".equals(opinion.getStatus())) {
            return;
        }

        List<SocialPlatformConfig> enabledPlatforms = configRepository.findByEnabledTrueAndShareOpinionsTrue();

        for (SocialPlatformConfig config : enabledPlatforms) {
            queueShare(SocialShareQueue.EntityType.OPINION, opinion.getId(), config.getPlatform());
        }

        // Process immediately
        processQueue();
    }

    /**
     * Add a share request to the queue if not already queued.
     */
    private void queueShare(SocialShareQueue.EntityType entityType, Long entityId,
            SocialPlatformConfig.Platform platform) {
        // Check if already queued or shared
        boolean exists = queueRepository.existsByEntityTypeAndEntityIdAndPlatformAndStatusIn(
                entityType, entityId, platform,
                List.of(SocialShareQueue.ShareStatus.PENDING, SocialShareQueue.ShareStatus.SUCCESS));

        if (exists) {
            log.debug("Share already queued for {} {} to {}", entityType, entityId, platform);
            return;
        }

        SocialShareQueue queue = SocialShareQueue.builder()
                .entityType(entityType)
                .entityId(entityId)
                .platform(platform)
                .status(SocialShareQueue.ShareStatus.PENDING)
                .build();

        queueRepository.save(queue);
        log.info("Queued {} {} for sharing to {}", entityType, entityId, platform);
    }

    /**
     * Process pending share requests.
     */
    @Transactional
    public void processQueue() {
        List<SocialShareQueue> pending = queueRepository.findPendingShares();

        for (SocialShareQueue queue : pending) {
            processShareRequest(queue);
        }
    }

    /**
     * Process a single share request.
     */
    private void processShareRequest(SocialShareQueue queue) {
        queue.setStatus(SocialShareQueue.ShareStatus.PROCESSING);
        queue.incrementAttempts();
        queueRepository.save(queue);

        try {
            String externalId = null;

            switch (queue.getPlatform()) {
                case TELEGRAM -> externalId = publishToTelegram(queue);
                case FACEBOOK -> externalId = publishToFacebook(queue);
                case TWITTER -> log.warn("Twitter publisher not implemented yet");
                case LINKEDIN -> log.warn("LinkedIn publisher not implemented yet");
                case WHATSAPP -> log.warn("WhatsApp publisher not implemented yet");
            }

            if (externalId != null) {
                queue.markSuccess(externalId);
                log.info("Successfully shared {} {} to {}", queue.getEntityType(), queue.getEntityId(),
                        queue.getPlatform());
            } else {
                queue.markFailed("No external ID returned");
            }
        } catch (Exception e) {
            log.error("Failed to share {} {} to {}: {}",
                    queue.getEntityType(), queue.getEntityId(), queue.getPlatform(), e.getMessage());
            queue.markFailed(e.getMessage());
        }

        queueRepository.save(queue);
    }

    /**
     * Publish content to Telegram.
     */
    private String publishToTelegram(SocialShareQueue queue) {
        String title, content, slug, coverImage;

        if (queue.getEntityType() == SocialShareQueue.EntityType.NEWS) {
            News news = newsRepository.findById(queue.getEntityId()).orElse(null);
            if (news == null) {
                log.error("News entity not found for id: {}", queue.getEntityId());
                return null;
            }
            title = news.getTitle();
            content = news.getContent();
            slug = "news/" + news.getSlug();
            coverImage = news.getCoverImage();
        } else {
            Opinion opinion = opinionRepository.findById(queue.getEntityId()).orElse(null);
            if (opinion == null) {
                log.error("Opinion entity not found for id: {}", queue.getEntityId());
                return null;
            }
            title = opinion.getTitle();
            content = opinion.getContent();
            slug = "opinions/" + opinion.getSlug();
            coverImage = opinion.getCoverImage();
        }

        String url = baseUrl + "/" + slug;

        // Handle image URL if necessary (ensure it's absolute if needed, or null if
        // local path not accessible by Telegram)
        // Telegram supports sending photos via URL if accessible, or we can just send
        // text.
        // For now, simpler to just send text link, or maybe image if it is a full URL.
        if (coverImage != null && !coverImage.startsWith("http")) {
            // It's a relative path likely, Telegram can't access localhost
            // So default to null
            coverImage = null;
        }

        return telegramPublisher.publish(title, cleanHtml(content), url, coverImage);
    }

    private String cleanHtml(String html) {
        if (html == null)
            return "";
        // Replace paragraph breaks and line breaks with newlines
        String text = html.replaceAll("</p>", "\n\n")
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</div>", "\n");
        // Remove all other tags
        String stripped = text.replaceAll("<[^>]*>", "").trim();
        // Unescape common HTML entities manually to avoid dependency issues
        return stripped
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    /**
     * Publish content to Facebook.
     */
    private String publishToFacebook(SocialShareQueue queue) {
        String title, excerpt, slug;

        if (queue.getEntityType() == SocialShareQueue.EntityType.NEWS) {
            News news = newsRepository.findById(queue.getEntityId()).orElse(null);
            if (news == null) {
                log.error("News entity not found for id: {}", queue.getEntityId());
                return null;
            }
            title = news.getTitle();
            excerpt = news.getExcerpt(); // Facebook prefers short text + link
            slug = "news/" + news.getSlug();
        } else {
            Opinion opinion = opinionRepository.findById(queue.getEntityId()).orElse(null);
            if (opinion == null) {
                log.error("Opinion entity not found for id: {}", queue.getEntityId());
                return null;
            }
            title = opinion.getTitle();
            excerpt = opinion.getExcerpt();
            slug = "opinions/" + opinion.getSlug();
        }

        String url = baseUrl + "/" + slug;
        String message = String.format("%s\n\n%s", title, excerpt);

        return facebookPublisher.publish(message, url);
    }

    /**
     * Retry failed shares periodically.
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void retryFailedShares() {
        List<SocialShareQueue> retryable = queueRepository.findRetryableShares(MAX_RETRY_ATTEMPTS);

        for (SocialShareQueue queue : retryable) {
            queue.setStatus(SocialShareQueue.ShareStatus.PENDING);
            queueRepository.save(queue);
        }

        if (!retryable.isEmpty()) {
            log.info("Retrying {} failed shares", retryable.size());
            processQueue();
        }
    }

    /**
     * Get share statistics.
     */
    public ShareStats getStats() {
        return ShareStats.builder()
                .pending(queueRepository.countByStatus(SocialShareQueue.ShareStatus.PENDING))
                .success(queueRepository.countByStatus(SocialShareQueue.ShareStatus.SUCCESS))
                .failed(queueRepository.countByStatus(SocialShareQueue.ShareStatus.FAILED))
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class ShareStats {
        private long pending;
        private long success;
        private long failed;
    }
}
