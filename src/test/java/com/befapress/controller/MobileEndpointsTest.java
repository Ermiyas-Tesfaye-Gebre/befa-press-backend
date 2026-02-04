package com.befapress.controller;

import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.entity.Role;
import com.befapress.entity.User;
import com.befapress.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private OpinionRepository opinionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BookmarkRepository bookmarkRepository;
    @Autowired
    private ArticleLikeRepository articleLikeRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private News testNews;
    private Opinion testOpinion;

    @BeforeEach
    void setUp() {
        // Create Role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .description("User role")
                        .build()));

        // Create User
        testUser = userRepository.findByEmail("mobile@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Mobile Test User")
                        .email("mobile@test.com")
                        .passwordHash(passwordEncoder.encode("password"))
                        .role(userRole)
                        .status("ACTIVE")
                        .isEmailVerified(true)
                        .build()));

        // Create News
        testNews = newsRepository.save(News.builder()
                .title("Mobile Test News")
                .slug("mobile-test-news")
                .excerpt("Excerpt")
                .content("Content")
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                // Need a category? Assuming existing or nullable for test simplicity,
                // but checking NewsControllerTest showed category is needed.
                // Re-using category creation logic would be safer.
                .category(categoryRepository.save(com.befapress.entity.Category.builder()
                        .name("Mobile Cat").slug("mobile-cat").status("ACTIVE").build()))
                .build());

        // Create Opinion
        testOpinion = opinionRepository.save(Opinion.builder()
                .title("Mobile Test Opinion")
                .slug("mobile-test-opinion")
                .excerpt("Excerpt")
                .content("Content")
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                .build());
    }

    // ==================== BOOKMARK TESTS ====================

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void bookmarkNews_ShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/bookmarks/news/" + testNews.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("NEWS"))
                .andExpect(jsonPath("$.itemId").value(testNews.getId()));

        assert bookmarkRepository.existsByUserAndNews(testUser, testNews);
    }

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void checkBookmarkStatus_ShouldReturnTrue_WhenBookmarked() throws Exception {
        bookmarkNews_ShouldSucceed(); // Create bookmark first

        mockMvc.perform(get("/api/v1/bookmarks/news/" + testNews.getId() + "/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    // ==================== LIKE TESTS ====================

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void likeNews_ShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/news/" + testNews.getId() + "/like"))
                .andExpect(status().isOk());

        assert articleLikeRepository.existsByUserAndNews(testUser, testNews);
    }

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void getLikeStatus_ShouldReturnCorrectStatus() throws Exception {
        likeNews_ShouldSucceed(); // Like first

        mockMvc.perform(get("/api/v1/news/" + testNews.getId() + "/like-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void unlikeOpinion_ShouldSucceed() throws Exception {
        // Like first manually
        mockMvc.perform(post("/api/v1/opinions/" + testOpinion.getId() + "/like"));

        // Then unlike
        mockMvc.perform(delete("/api/v1/opinions/" + testOpinion.getId() + "/like"))
                .andExpect(status().isOk());

        assert !articleLikeRepository.existsByUserAndOpinion(testUser, testOpinion);
    }

    // ==================== PUSH NOTIFICATION TESTS ====================

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void registerFcmToken_ShouldSucceed() throws Exception {
        String jsonRequest = "{\"token\":\"test-fcm-token-123\", \"platform\":\"ANDROID\", \"deviceName\":\"Pixel 6\"}";

        mockMvc.perform(post("/api/v1/notifications/fcm-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());

        assert deviceTokenRepository.findByToken("test-fcm-token-123").isPresent();
    }

    // ==================== USER PREFERENCES TESTS ====================

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void getPreferences_ShouldReturnDefaults_WhenNotSet() throws Exception {
        mockMvc.perform(get("/api/v1/user/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushEnabled").value(true))
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    @WithMockUser(username = "mobile@test.com", roles = "USER")
    void updatePreferences_ShouldUpdateValues() throws Exception {
        String jsonRequest = "{\"language\":\"am\", \"darkMode\":true}";

        mockMvc.perform(put("/api/v1/user/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("am"))
                .andExpect(jsonPath("$.darkMode").value(true));

        // Verify defaults kept
        mockMvc.perform(get("/api/v1/user/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushEnabled").value(true)); // Should still be default true
    }
}
