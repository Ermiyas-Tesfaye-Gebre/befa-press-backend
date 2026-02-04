package com.befapress.scheduler;

import com.befapress.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for subscription lifecycle management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Daily job at 6:00 AM to process expiring and expired subscriptions
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void processSubscriptionLifecycle() {
        log.info("Starting daily subscription lifecycle check...");

        try {
            // Process subscriptions expiring in 3 days
            subscriptionService.processExpiringSubscriptions();
            log.info("Processed expiring subscriptions");

            // Mark expired subscriptions
            subscriptionService.processExpiredSubscriptions();
            log.info("Processed expired subscriptions");
        } catch (Exception e) {
            log.error("Error in subscription lifecycle processing", e);
        }
    }

    /**
     * Weekly job every Monday at 9:00 AM to send promotional emails
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyPromotionalEmails() {
        log.info("Starting weekly promotional email send...");

        try {
            subscriptionService.sendWeeklyPromotionalEmails();
            log.info("Completed weekly promotional emails");
        } catch (Exception e) {
            log.error("Error sending promotional emails", e);
        }
    }
}
