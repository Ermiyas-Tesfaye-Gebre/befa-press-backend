package com.befapress.service;

import com.befapress.dto.request.SubscribeRequest;
import com.befapress.dto.response.SubscriptionResponse;
import com.befapress.entity.News;
import com.befapress.entity.Subscription;
import com.befapress.entity.SubscriptionStatus;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing email subscriptions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final NewsRepository newsRepository;
    private final EmailService emailService;

    private static final int SUBSCRIPTION_DAYS = 30;
    private static final int EXPIRY_REMINDER_DAYS = 3;

    /**
     * Subscribe a new email address
     */
    @Transactional
    public SubscriptionResponse subscribe(SubscribeRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (subscriptionRepository.existsByEmail(email)) {
            Subscription existing = subscriptionRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "email", email));

            if (existing.getStatus() == SubscriptionStatus.ACTIVE ||
                    existing.getStatus() == SubscriptionStatus.EXPIRING_SOON) {
                throw new BadRequestException("Email is already subscribed");
            }

            // Reactivate expired subscription
            existing.setStatus(SubscriptionStatus.ACTIVE);
            existing.setStartDate(LocalDateTime.now());
            existing.setEndDate(LocalDateTime.now().plusDays(SUBSCRIPTION_DAYS));
            subscriptionRepository.save(existing);

            sendConfirmationEmail(existing);
            log.info("Reactivated subscription for: {}", email);
            return mapToResponse(existing);
        }

        Subscription subscription = Subscription.builder()
                .email(email)
                .username(Subscription.extractUsername(email))
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(SUBSCRIPTION_DAYS))
                .build();

        subscriptionRepository.save(subscription);
        sendConfirmationEmail(subscription);

        log.info("New subscription created for: {}", email);
        return mapToResponse(subscription);
    }

    /**
     * Get subscription status by email
     */
    public SubscriptionResponse getStatus(String email) {
        Subscription subscription = subscriptionRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "email", email));
        return mapToResponse(subscription);
    }

    /**
     * Get all subscriptions (admin)
     */
    public Page<SubscriptionResponse> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Extend subscription (admin)
     */
    @Transactional
    public SubscriptionResponse extendSubscription(Long id, int days) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));

        LocalDateTime newEndDate = subscription.getEndDate().plusDays(days);
        subscription.setEndDate(newEndDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        log.info("Extended subscription {} by {} days", id, days);
        return mapToResponse(subscription);
    }

    /**
     * Update subscription status (admin)
     */
    @Transactional
    public SubscriptionResponse updateStatus(Long id, SubscriptionStatus status) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));

        subscription.setStatus(status);
        subscriptionRepository.save(subscription);

        log.info("Updated subscription {} status to {}", id, status);
        return mapToResponse(subscription);
    }

    /**
     * Process expiring subscriptions (scheduled)
     */
    @Transactional
    public void processExpiringSubscriptions() {
        LocalDateTime reminderDate = LocalDateTime.now().plusDays(EXPIRY_REMINDER_DAYS);
        LocalDateTime today = LocalDateTime.now();

        // Find subscriptions expiring in 3 days
        List<Subscription> expiringSoon = subscriptionRepository.findByEndDateBetween(
                today, reminderDate);

        for (Subscription sub : expiringSoon) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                sub.setStatus(SubscriptionStatus.EXPIRING_SOON);
                subscriptionRepository.save(sub);
                sendExpiryReminderEmail(sub);
                log.info("Sent expiry reminder to: {}", sub.getEmail());
            }
        }
    }

    /**
     * Process expired subscriptions (scheduled)
     */
    @Transactional
    public void processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> expired = subscriptionRepository.findByEndDateBeforeAndStatus(
                now, SubscriptionStatus.ACTIVE);
        expired.addAll(subscriptionRepository.findByEndDateBeforeAndStatus(
                now, SubscriptionStatus.EXPIRING_SOON));

        for (Subscription sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            sendExpiredEmail(sub);
            log.info("Marked subscription as expired: {}", sub.getEmail());
        }
    }

    /**
     * Send promotional emails to active subscribers (scheduled weekly)
     */
    @Transactional
    public void sendWeeklyPromotionalEmails() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        List<Subscription> subscribers = subscriptionRepository
                .findByStatusAndLastPromotionalEmailBeforeOrLastPromotionalEmailIsNull(
                        SubscriptionStatus.ACTIVE, oneWeekAgo);

        // Get latest news for promotion
        List<News> latestNews = newsRepository.findTop5ByStatusOrderByPublishedAtDesc("PUBLISHED");

        for (Subscription sub : subscribers) {
            sendPromotionalEmail(sub, latestNews);
            sub.setLastPromotionalEmail(LocalDateTime.now());
            subscriptionRepository.save(sub);
            log.info("Sent promotional email to: {}", sub.getEmail());
        }
    }

    // ========== Email Methods ==========

    private void sendConfirmationEmail(Subscription subscription) {
        String subject = "Welcome to BEFA Press Subscription";
        String content = buildConfirmationEmailContent(subscription);
        emailService.sendHtmlEmail(subscription.getEmail(), subject, content);
    }

    private void sendExpiryReminderEmail(Subscription subscription) {
        String subject = "Your BEFA Press Subscription Expires Soon";
        String content = buildExpiryReminderEmailContent(subscription);
        emailService.sendHtmlEmail(subscription.getEmail(), subject, content);
    }

    private void sendExpiredEmail(Subscription subscription) {
        String subject = "Your BEFA Press Subscription Has Expired";
        String content = buildExpiredEmailContent(subscription);
        emailService.sendHtmlEmail(subscription.getEmail(), subject, content);
    }

    private void sendPromotionalEmail(Subscription subscription, List<News> news) {
        String subject = "BEFA Press Weekly Digest";
        String content = buildPromotionalEmailContent(subscription, news);
        emailService.sendHtmlEmail(subscription.getEmail(), subject, content);
    }

    // ========== Email Content Builders ==========

    private String buildConfirmationEmailContent(Subscription subscription) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                    <div style="padding: 20px;">
                        <h1 style="color: #1a1a1a;">Welcome to BEFA Press</h1>
                        <p>Dear %s,</p>
                        <p>Thank you for subscribing to <strong>BEFA Press</strong> – Breaking Ethiopian Facts & Articles.</p>

                        <div style="background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p><strong>Your Subscription Details:</strong></p>
                            <p>📅 Start Date: %s</p>
                            <p>📅 End Date: %s</p>
                        </div>

                        <p>You will receive curated Ethiopian news and intellectual opinions directly to your inbox.</p>

                        <div style="background: #fff3cd; padding: 15px; border-radius: 8px; margin: 20px 0; border: 1px solid #ffc107;">
                            <p><strong>💳 Payment Options (Coming Soon):</strong></p>
                            <p>🔹 <a href="#">Telebirr</a> | 🔹 <a href="#">CBE (Commercial Bank of Ethiopia)</a></p>
                            <p style="font-size: 12px; color: #666;">⚠️ Payment integration will be implemented soon.</p>
                        </div>

                        <p>Thank you for supporting independent Ethiopian journalism.</p>
                        <p>— BEFA Press Team</p>
                    </div>
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                </body>
                </html>
                """
                .formatted(
                        subscription.getUsername(),
                        subscription.getStartDate().toLocalDate(),
                        subscription.getEndDate().toLocalDate());
    }

    private String buildExpiryReminderEmailContent(Subscription subscription) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                    <div style="padding: 20px;">
                        <h1 style="color: #F59E0B;">⏰ Subscription Expiring Soon</h1>
                        <p>Dear %s,</p>
                        <p>Your BEFA Press subscription will expire on <strong>%s</strong>.</p>

                        <p>Don't miss out on quality Ethiopian journalism!</p>

                        <div style="background: #fff3cd; padding: 15px; border-radius: 8px; margin: 20px 0; border: 1px solid #ffc107;">
                            <p><strong>💳 Renew Your Subscription:</strong></p>
                            <p>🔹 <a href="#">Telebirr</a> | 🔹 <a href="#">CBE (Commercial Bank of Ethiopia)</a></p>
                            <p style="font-size: 12px; color: #666;">⚠️ Payment integration will be implemented soon.</p>
                        </div>

                        <p>— BEFA Press Team</p>
                    </div>
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                </body>
                </html>
                """
                .formatted(
                        subscription.getUsername(),
                        subscription.getEndDate().toLocalDate());
    }

    private String buildExpiredEmailContent(Subscription subscription) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                    <div style="padding: 20px;">
                        <h1 style="color: #EF4444;">Subscription Expired</h1>
                        <p>Dear %s,</p>
                        <p>Your BEFA Press subscription has expired.</p>

                        <p>We hope you enjoyed our content! To continue receiving Ethiopian news and opinions, please renew your subscription.</p>

                        <div style="background: #f0f0f0; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p><strong>💳 Renew Now:</strong></p>
                            <p>🔹 <a href="#">Telebirr</a> | 🔹 <a href="#">CBE (Commercial Bank of Ethiopia)</a></p>
                            <p style="font-size: 12px; color: #666;">⚠️ Payment integration will be implemented soon.</p>
                        </div>

                        <p>We hope to see you back soon!</p>
                        <p>— BEFA Press Team</p>
                    </div>
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                </body>
                </html>
                """
                .formatted(subscription.getUsername());
    }

    private String buildPromotionalEmailContent(Subscription subscription, List<News> newsList) {
        StringBuilder newsItems = new StringBuilder();
        for (News news : newsList) {
            newsItems.append("""
                    <div style="border-bottom: 1px solid #eee; padding: 10px 0;">
                        <h3 style="margin: 0; color: #1a1a1a;">%s</h3>
                        <p style="color: #666; font-size: 14px;">%s</p>
                    </div>
                    """.formatted(news.getTitle(), news.getExcerpt() != null ? news.getExcerpt() : ""));
        }

        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                    <div style="padding: 20px;">
                        <h1 style="color: #16A34A;">📰 BEFA Press Weekly Digest</h1>
                        <p>Dear %s,</p>
                        <p>Here are this week's top stories from BEFA Press:</p>

                        <div style="margin: 20px 0;">
                            %s
                        </div>

                        <p><a href="#" style="background: #16A34A; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Read More on BEFA Press</a></p>

                        <p style="color: #888; font-size: 12px; margin-top: 30px;">
                            You are receiving this email because you subscribed to BEFA Press.
                        </p>
                        <p>— BEFA Press Team</p>
                    </div>
                    <div style="background: linear-gradient(to right, #16A34A, #F59E0B, #EF4444); height: 5px;"></div>
                </body>
                </html>
                """
                .formatted(subscription.getUsername(), newsItems.toString());
    }

    // ========== Helper Methods ==========

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .email(subscription.getEmail())
                .username(subscription.getUsername())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
