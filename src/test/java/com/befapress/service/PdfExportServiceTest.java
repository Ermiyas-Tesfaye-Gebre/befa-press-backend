package com.befapress.service;

import com.befapress.entity.Category;
import com.befapress.entity.News;
import com.befapress.entity.User;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class PdfExportServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private OpinionRepository opinionRepository;

    @InjectMocks
    private PdfExportService pdfExportService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(pdfExportService, "appName", "BEFA Press");
    }

    @Test
    public void testExportNewsToPdf_Basic() {
        Long newsId = 1L;
        News news = new News();
        news.setId(newsId);
        news.setTitle("Test Title");
        news.setContent("<p>Test content</p>");
        news.setSlug("test-news");
        news.setPublishedAt(LocalDateTime.now());
        // For boolean isBreaking, lombok usually generates setBreaking or setIsBreaking
        // depending on config.
        // Assuming standard lombok behavior for private boolean isBreaking
        news.setBreaking(true);
        news.setCoverImage(null);

        Category category = new Category();
        category.setName("Politics");
        news.setCategory(category);

        User author = new User();
        author.setFullName("John Doe");
        news.setAuthor(author);

        when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));

        byte[] result = pdfExportService.exportNewsToPdf(newsId);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
