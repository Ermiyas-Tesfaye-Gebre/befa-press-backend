package com.befapress.service.social;

import com.befapress.entity.SocialPlatformConfig;
import com.befapress.repository.SocialPlatformConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Facebook Graph API Publisher.
 * Posts content to a Facebook Page.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookPublisher {

    private static final String FB_GRAPH_API_URL = "https://graph.facebook.com/v19.0/%s/feed";

    private final SocialPlatformConfigRepository configRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a post to Facebook Page.
     *
     * @param message The main text content
     * @param link    The URL to share
     * @return External post ID if successful, null otherwise
     */
    public String publish(String message, String link) {
        Optional<SocialPlatformConfig> configOpt = configRepository
                .findByPlatform(SocialPlatformConfig.Platform.FACEBOOK);

        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            log.warn("Facebook is not configured or disabled");
            return null;
        }

        SocialPlatformConfig config = configOpt.get();

        try {
            JsonNode credentials = objectMapper.readTree(config.getCredentials());
            String pageId = credentials.has("pageId") ? credentials.get("pageId").asText() : "";
            String accessToken = credentials.has("accessToken") ? credentials.get("accessToken").asText() : "";

            if (!StringUtils.hasText(pageId) || !StringUtils.hasText(accessToken)) {
                log.error("Facebook Page ID or Access Token is missing");
                return null;
            }

            String apiUrl = String.format(FB_GRAPH_API_URL, pageId);

            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            body.put("link", link);
            body.put("access_token", accessToken);
            body.put("published", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            if (response != null && response.containsKey("id")) {
                String postId = (String) response.get("id");
                log.info("Successfully published to Facebook Page: {}", postId);
                return postId;
            } else {
                log.error("Failed to publish to Facebook. Response: {}", response);
                return null;
            }

        } catch (Exception e) {
            log.error("Facebook publish failed", e);
            throw new RuntimeException("Facebook publish failed: " + e.getMessage());
        }
    }
}
