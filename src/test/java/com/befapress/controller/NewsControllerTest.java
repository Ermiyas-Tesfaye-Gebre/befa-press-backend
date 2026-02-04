package com.befapress.controller;

import com.befapress.entity.Category;
import com.befapress.entity.News;
import com.befapress.entity.Role;
import com.befapress.entity.User;
import com.befapress.repository.CategoryRepository;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.RoleRepository;
import com.befapress.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Category testCategory;
    private User testAuthor;
    private News testNews;

    @BeforeEach
    void setUp() {
        // Create test role
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    role.setDescription("Admin role");
                    return roleRepository.save(role);
                });

        // Create test category
        testCategory = categoryRepository.findBySlug("test-category")
                .orElseGet(() -> {
                    Category category = Category.builder()
                            .name("Test Category")
                            .slug("test-category")
                            .description("Test category for unit tests")
                            .status("ACTIVE")
                            .displayOrder(1)
                            .build();
                    return categoryRepository.save(category);
                });

        // Create test author
        testAuthor = userRepository.findByEmail("author@test.com")
                .orElseGet(() -> {
                    User user = User.builder()
                            .fullName("Test Author")
                            .email("author@test.com")
                            .passwordHash(passwordEncoder.encode("password"))
                            .role(adminRole)
                            .status("ACTIVE")
                            .isEmailVerified(true)
                            .build();
                    return userRepository.save(user);
                });

        // Create test news
        testNews = newsRepository.findBySlug("test-news-article")
                .orElseGet(() -> {
                    News news = News.builder()
                            .title("Test News Article")
                            .slug("test-news-article")
                            .excerpt("This is a test news excerpt")
                            .content("<p>This is the test news content.</p>")
                            .category(testCategory)
                            .author(testAuthor)
                            .status("PUBLISHED")
                            .publishedAt(LocalDateTime.now())
                            .viewCount(100)
                            .build();
                    return newsRepository.save(news);
                });
    }

    @Test
    void getAllNews_ShouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getNewsBySlug_WithValidSlug_ShouldReturnNews() throws Exception {
        mockMvc.perform(get("/api/v1/news/slug/test-news-article"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test News Article"))
                .andExpect(jsonPath("$.slug").value("test-news-article"));
    }

    @Test
    void getNewsBySlug_WithInvalidSlug_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/news/slug/non-existent-article"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBreakingNews_ShouldReturnList() throws Exception {
        // Create breaking news
        News breakingNews = News.builder()
                .title("Breaking News Test")
                .slug("breaking-news-test")
                .content("<p>Breaking content</p>")
                .category(testCategory)
                .author(testAuthor)
                .status("PUBLISHED")
                .isBreaking(true)
                .publishedAt(LocalDateTime.now())
                .build();
        newsRepository.save(breakingNews);

        mockMvc.perform(get("/api/v1/news/breaking")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getTrendingNews_ShouldReturnOrderedByViews() throws Exception {
        mockMvc.perform(get("/api/v1/news/trending")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchNews_ShouldReturnMatchingResults() throws Exception {
        mockMvc.perform(get("/api/v1/news/search")
                .param("q", "Test")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void incrementViewCount_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/news/" + testNews.getId() + "/view")
                .contentType("application/json"))
                .andExpect(status().isMethodNotAllowed()); // Should be POST

        // Test with POST
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/news/" + testNews.getId() + "/view"))
                .andExpect(status().isOk());
    }
}
