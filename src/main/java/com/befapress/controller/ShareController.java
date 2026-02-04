package com.befapress.controller;

import com.befapress.dto.response.MessageResponse;
import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
@Tag(name = "Share", description = "Social sharing APIs")
public class ShareController {

    private final NewsRepository newsRepository;
    private final OpinionRepository opinionRepository;

    @Value("${app.base-url:https://befapress.com}")
    private String baseUrl;

    @GetMapping("/news/{id}")
    @Operation(summary = "Get sharing URLs for a news article")
    public ResponseEntity<Map<String, String>> getNewsShareLinks(@PathVariable Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));

        String articleUrl = baseUrl + "/news/" + news.getSlug();
        String title = news.getTitle();
        String excerpt = news.getExcerpt() != null ? news.getExcerpt() : "";

        return ResponseEntity.ok(buildShareLinks(articleUrl, title, excerpt));
    }

    @GetMapping("/opinion/{id}")
    @Operation(summary = "Get sharing URLs for an opinion article")
    public ResponseEntity<Map<String, String>> getOpinionShareLinks(@PathVariable Long id) {
        Opinion opinion = opinionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", id));

        String articleUrl = baseUrl + "/opinion/" + opinion.getSlug();
        String title = opinion.getTitle();
        String excerpt = opinion.getExcerpt() != null ? opinion.getExcerpt() : "";

        return ResponseEntity.ok(buildShareLinks(articleUrl, title, excerpt));
    }

    @PostMapping("/news/{id}/track")
    @Operation(summary = "Track a share action for analytics")
    public ResponseEntity<MessageResponse> trackNewsShare(
            @PathVariable Long id,
            @RequestParam String platform) {
        // In a full implementation, you would log this to a shares table
        // For now, just acknowledge the share
        return ResponseEntity.ok(MessageResponse.success("Share tracked for platform: " + platform));
    }

    private Map<String, String> buildShareLinks(String url, String title, String excerpt) {
        Map<String, String> links = new HashMap<>();

        String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedExcerpt = URLEncoder.encode(excerpt, StandardCharsets.UTF_8);

        // Facebook
        links.put("facebook", "https://www.facebook.com/sharer/sharer.php?u=" + encodedUrl);

        // Twitter/X
        links.put("twitter", "https://twitter.com/intent/tweet?url=" + encodedUrl + "&text=" + encodedTitle);

        // LinkedIn
        links.put("linkedin", "https://www.linkedin.com/shareArticle?mini=true&url=" + encodedUrl + "&title="
                + encodedTitle + "&summary=" + encodedExcerpt);

        // WhatsApp
        links.put("whatsapp", "https://api.whatsapp.com/send?text=" + encodedTitle + "%20" + encodedUrl);

        // Telegram
        links.put("telegram", "https://t.me/share/url?url=" + encodedUrl + "&text=" + encodedTitle);

        // Email
        links.put("email", "mailto:?subject=" + encodedTitle + "&body=" + encodedExcerpt + "%0A%0A" + encodedUrl);

        // Copy link
        links.put("copyLink", url);

        return links;
    }
}
