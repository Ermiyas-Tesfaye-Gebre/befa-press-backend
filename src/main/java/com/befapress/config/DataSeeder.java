package com.befapress.config;

import com.befapress.entity.Role;
import com.befapress.entity.User;
import com.befapress.repository.CategoryRepository;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import com.befapress.repository.RoleRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.befapress.entity.Notification;
import com.befapress.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data seeder to populate the database with initial data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final CategoryRepository categoryRepository;
        private final NewsRepository newsRepository;
        private final OpinionRepository opinionRepository;
        private final JdbcTemplate jdbcTemplate;
        // New Repositories for Dashboard Seeding
        private final com.befapress.repository.CommentRepository commentRepository;
        private final com.befapress.repository.CommentReportRepository reportRepository;
        private final com.befapress.repository.ActivityLogRepository activityLogRepository;
        private final com.befapress.repository.PageHitRepository pageHitRepository;
        private final NotificationRepository notificationRepository;
        // Social Config
        private final com.befapress.repository.SocialPlatformConfigRepository socialConfigRepository;
        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Override
        public void run(String... args) throws Exception {
                try {
                        // Fix for ip_address column length - FORCE EXECUTION
                        try {
                                log.info("Attempting to modify ip_address column length...");
                                jdbcTemplate.execute("ALTER TABLE comments MODIFY COLUMN ip_address VARCHAR(100)");
                                log.info("SUCCESS: Schema update executed for ip_address column.");
                        } catch (Exception e) {
                                log.error("SCHEMA UPDATE FAILED: Could not modify ip_address column. Error: {}",
                                                e.getMessage());
                        }

                        seedRoles();
                        seedUsers();
                        seedCategories();
                        seedNews();
                        seedOpinions();
                        // Dashboard Data
                        seedPendingData();
                        seedActivityLogs();
                        seedPageHits();
                        seedNotifications();
                        seedSocialConfig();
                } catch (Exception e) {
                        log.error("DATA SEEDING FAILED: Application will continue, but data might be missing.", e);
                }
        }

        private void seedRoles() {
                List<String> roles = Arrays.asList(
                                "ROLE_USER",
                                "ROLE_INTELLECTUAL",
                                "ROLE_EDITOR",
                                "ROLE_REVIEWER",
                                "ROLE_FINANCE_OFFICER",
                                "ROLE_AUDITOR",
                                "ROLE_ADMIN",
                                "ROLE_SUPER_ADMIN");

                for (String roleName : roles) {
                        if (!roleRepository.existsByName(roleName)) {
                                Role role = Role.builder()
                                                .name(roleName)
                                                .description("Role for " + roleName.replace("ROLE_", "").toLowerCase())
                                                .build();
                                roleRepository.save(role);
                                log.info("Seeded role: {}", roleName);
                        }
                }
        }

        private void seedUsers() {
                // Create Super Admin if not exists
                // Create or Update Super Admin
                String adminEmail = "admin@befapress.com";
                Role adminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

                User admin = userRepository.findByEmail(adminEmail).orElse(null);

                if (admin == null) {
                        admin = User.builder()
                                        .fullName("Super Admin")
                                        .email(adminEmail)
                                        .passwordHash(passwordEncoder.encode("Admin@123"))
                                        .role(adminRole)
                                        .status("ACTIVE")
                                        .isEmailVerified(true)
                                        .isVerified(true)
                                        .bio("System Administrator")
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        userRepository.save(admin);
                        log.info("Seeded super admin user: {}", adminEmail);
                } else {
                        // Ensure password is correct (Force Reset for testing/recovery)
                        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                        admin.setRole(adminRole); // Ensure role is correct
                        admin.setStatus("ACTIVE"); // Ensure active
                        userRepository.save(admin);
                        log.info("Updated super admin credentials: {}", adminEmail);
                }

                // Create Intellectual User if not exists
                String intellectualEmail = "intellectual@befapress.com";
                if (!userRepository.existsByEmail(intellectualEmail)) {
                        Role intellectualRole = roleRepository.findByName("ROLE_INTELLECTUAL")
                                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

                        User intellectual = User.builder()
                                        .fullName("Dr. Wise Man")
                                        .email(intellectualEmail)
                                        .passwordHash(passwordEncoder.encode("Writer@123"))
                                        .role(intellectualRole)
                                        .status("ACTIVE")
                                        .isEmailVerified(true)
                                        .isVerified(true)
                                        .expertiseField("Economics")
                                        .affiliation("Addis Ababa University")
                                        .bio("Expert in Ethiopian Economics")
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        userRepository.save(intellectual);
                        log.info("Seeded intellectual user: {}", intellectualEmail);
                }
        }

        private void seedCategories() {
                if (!categoryRepository.existsBySlug("politics")) {
                        com.befapress.entity.Category politics = com.befapress.entity.Category.builder()
                                        .name("Politics")
                                        .slug("politics")
                                        .description("Political news and analysis")
                                        .status("ACTIVE")
                                        .displayOrder(1)
                                        .build();
                        categoryRepository.save(politics);
                        log.info("Seeded category: Politics");
                }

                if (!categoryRepository.existsBySlug("economy")) {
                        com.befapress.entity.Category economy = com.befapress.entity.Category.builder()
                                        .name("Economy")
                                        .slug("economy")
                                        .description("Economic news and updates")
                                        .status("ACTIVE")
                                        .displayOrder(2)
                                        .build();
                        categoryRepository.save(economy);
                        log.info("Seeded category: Economy");
                }
        }

        private void seedNews() {
                User author = userRepository.findByEmail("intellectual@befapress.com")
                                .orElseThrow(() -> new RuntimeException("Author not found"));
                com.befapress.entity.Category category = categoryRepository.findBySlug("politics")
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                if (!newsRepository.existsBySlug("future-of-ethiopian-economy")) {
                        com.befapress.entity.News news = com.befapress.entity.News.builder()
                                        .title("The Future of Ethiopian Economy")
                                        .slug("future-of-ethiopian-economy")
                                        .content("<h1>Economic Reforms Needed</h1><p>Detailed analysis of the current economic situation and proposed reforms...</p>")
                                        .excerpt("Detailed analysis of the current economic situation.")
                                        .author(author)
                                        .category(category)
                                        .status("PUBLISHED")
                                        .isFeatured(true)
                                        .isTrending(true)
                                        .publishedAt(LocalDateTime.now())
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        newsRepository.save(news);
                        log.info("Seeded news article: {}", news.getTitle());
                }

                if (!newsRepository.existsBySlug("ethiopia-announces-major-economic-reforms")) {
                        com.befapress.entity.News breakingNews = com.befapress.entity.News.builder()
                                        .title("Ethiopia Announces Major Economic Reforms")
                                        .slug("ethiopia-announces-major-economic-reforms")
                                        .content("<h1>New Economic Era</h1><p>The government has unveiled a comprehensive package of economic reforms...</p>")
                                        .excerpt("Major step towards economic liberalization.")
                                        .author(author)
                                        .category(category)
                                        .status("PUBLISHED")
                                        .isFeatured(false)
                                        .isBreaking(true)
                                        .publishedAt(LocalDateTime.now())
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        newsRepository.save(breakingNews);
                        log.info("Seeded news article: {}", breakingNews.getTitle());
                }
        }

        private void seedOpinions() {
                User author = userRepository.findByEmail("intellectual@befapress.com")
                                .orElseThrow(() -> new RuntimeException("Author not found"));

                if (opinionRepository.count() == 0) {
                        com.befapress.entity.Opinion opinion = com.befapress.entity.Opinion.builder()
                                        .title("Why Technology Matters for Africa")
                                        .slug("why-technology-matters-for-africa")
                                        .content("<h1>Digital Transformation</h1><p>Africa typically lags in adoption, but leapfrogging is possible...</p>")
                                        .excerpt("An analysis of tech adoption in the continent.")
                                        .author(author)
                                        .status("APPROVED")
                                        .publishedAt(LocalDateTime.now())
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        opinionRepository.save(opinion);
                        log.info("Seeded opinion article: {}", opinion.getTitle());
                }
        }

        private void seedPendingData() {
                // 1. Pending News
                if (newsRepository.countByStatusAndDeletedAtIsNull("PENDING") == 0) {
                        User author = userRepository.findByEmail("intellectual@befapress.com").orElseThrow();
                        com.befapress.entity.Category category = categoryRepository.findBySlug("economy").orElseThrow();

                        com.befapress.entity.News pendingNews = com.befapress.entity.News.builder()
                                        .title("Draft: The Impact of AI on Local Markets")
                                        .slug("impact-of-ai-on-local-markets")
                                        .content("<p>Pending review content...</p>")
                                        .excerpt("Awaiting moderation.")
                                        .author(author)
                                        .category(category)
                                        .status("PENDING")
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        newsRepository.save(pendingNews);
                        log.info("Seeded PENDING news");
                }

                // 2. Flagged Comments (Reports)
                if (reportRepository.countByStatus("PENDING") == 0) {
                        // Need a comment first
                        com.befapress.entity.News news = newsRepository.findAll().get(0);
                        User user = userRepository.findByEmail("intellectual@befapress.com").orElseThrow();

                        com.befapress.entity.Comment comment = com.befapress.entity.Comment.builder()
                                        .content("This is a controversial comment.")
                                        .news(news)
                                        .user(user)
                                        .status("ACTIVE")
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        commentRepository.save(comment);

                        com.befapress.entity.CommentReport report = com.befapress.entity.CommentReport.builder()
                                        .comment(comment)
                                        .reporterEmail(user.getEmail())
                                        .reason("HATE_SPEECH")
                                        .description("Offensive language")
                                        .status("PENDING")
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        reportRepository.save(report);
                        log.info("Seeded PENDING comment report");
                }
        }

        private void seedActivityLogs() {
                if (activityLogRepository.count() == 0) {
                        activityLogRepository.save(com.befapress.entity.ActivityLog.builder()
                                        .type("NEWS")
                                        .message("New article 'Future of Ethiopia' submitted for review")
                                        .actor("Dr. Wise Man")
                                        .createdAt(LocalDateTime.now().minusMinutes(5))
                                        .build());

                        activityLogRepository.save(com.befapress.entity.ActivityLog.builder()
                                        .type("USER")
                                        .message("New user registration: Abebe Kebede")
                                        .actor("System")
                                        .createdAt(LocalDateTime.now().minusMinutes(20))
                                        .build());

                        activityLogRepository.save(com.befapress.entity.ActivityLog.builder()
                                        .type("COMMENT")
                                        .message("Comment flagged on 'Economic Reforms'")
                                        .actor("Moderator Bot")
                                        .createdAt(LocalDateTime.now().minusHours(1))
                                        .build());
                        log.info("Seeded Activity Logs");
                }
        }

        private void seedPageHits() {
                // Seed last 24h traffic if empty
                if (pageHitRepository.count() == 0) {
                        LocalDateTime now = LocalDateTime.now();
                        java.util.Random random = new java.util.Random();

                        // Generate ~50 hits spread over 24h
                        for (int i = 0; i < 50; i++) {
                                int hoursAgo = random.nextInt(24);
                                com.befapress.entity.PageHit hit = com.befapress.entity.PageHit.builder()
                                                .entityType("HOME")
                                                .ipAddress("192.168.1." + random.nextInt(255))
                                                .userAgent("Mozilla/5.0...")
                                                .device("DESKTOP")
                                                .sessionId("session-" + i)
                                                .createdAt(now.minusHours(hoursAgo))
                                                .build();
                                pageHitRepository.save(hit);
                        }
                        log.info("Seeded 50 PageHits for traffic chart");
                }
        }

        private void seedNotifications() {
                if (notificationRepository.count() == 0) {
                        notificationRepository.saveAll(List.of(
                                        Notification.builder()
                                                        .message("New article 'Future of Ethiopia' submitted for review")
                                                        .type(Notification.NotificationType.NEWS)
                                                        .isRead(false)
                                                        .build(),
                                        Notification.builder()
                                                        .message("New user registration: Abebe Kebede")
                                                        .type(Notification.NotificationType.USER)
                                                        .isRead(false)
                                                        .build(),
                                        Notification.builder()
                                                        .message("Comment flagged on 'Economic Reforms'")
                                                        .type(Notification.NotificationType.COMMENT)
                                                        .isRead(true)
                                                        .build()));
                        log.info("Seeded 3 sample notifications");
                }
        }

        private void seedSocialConfig() {
                // Seed Facebook Config
                if (socialConfigRepository.findByPlatform(com.befapress.entity.SocialPlatformConfig.Platform.FACEBOOK)
                                .isEmpty()) {
                        try {
                                java.util.Map<String, String> creds = new java.util.HashMap<>();
                                creds.put("appId", "2683520002046971");
                                creds.put("appSecret", "3cd61e76703a64aa78edbbb0109f1d1f");
                                creds.put("pageId", ""); // User needs to provide this later
                                creds.put("accessToken", ""); // User needs to provide this later

                                String jsonCreds = objectMapper.writeValueAsString(creds);

                                com.befapress.entity.SocialPlatformConfig fbConfig = com.befapress.entity.SocialPlatformConfig
                                                .builder()
                                                .platform(com.befapress.entity.SocialPlatformConfig.Platform.FACEBOOK)
                                                .enabled(false) // Disabled until Page ID/Token are set
                                                .credentials(jsonCreds)
                                                .channelUrl("https://facebook.com/befapress")
                                                .shareNews(true)
                                                .shareOpinions(true)
                                                .createdAt(LocalDateTime.now())
                                                .build();

                                socialConfigRepository.save(fbConfig);
                                log.info("Seeded Facebook configuration (Disabled - Waiting for credentials)");
                        } catch (Exception e) {
                                log.error("Failed to seed Facebook config", e);
                        }
                }
        }
}
