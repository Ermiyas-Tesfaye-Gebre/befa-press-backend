package com.befapress.service.social;

import com.befapress.entity.SocialPlatformConfig;
import com.befapress.entity.SocialShareQueue;
import com.befapress.repository.SocialPlatformConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Telegram Bot API Publisher.
 * Sends messages to configured Telegram channels.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramPublisher {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";
    private static final int MAX_MESSAGE_LENGTH = 4096;

    private final SocialPlatformConfigRepository configRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a message to Telegram channel.
     *
     * @param title    Article title
     * @param excerpt  Short description
     * @param url      Full article URL
     * @param imageUrl Optional cover image URL
     * @return External message ID if successful, null otherwise
     */
    public String publish(String title, String content, String url, String imageUrl) {
        Optional<SocialPlatformConfig> configOpt = configRepository
                .findByPlatform(SocialPlatformConfig.Platform.TELEGRAM);

        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            log.warn("Telegram is not configured or disabled");
            return null;
        }

        SocialPlatformConfig config = configOpt.get();

        try {
            JsonNode credentials = objectMapper.readTree(config.getCredentials());
            String botToken = credentials.get("botToken").asText();
            String channelId = credentials.get("channelId").asText();

            String message = formatMessage(title, content, url, config.getMessageTemplate());

            return sendMessage(botToken, channelId, message, imageUrl);
        } catch (Exception e) {
            log.error("Failed to publish to Telegram", e);
            throw new RuntimeException("Telegram publish failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send a message using Telegram Bot API.
     */
    private String sendMessage(String botToken, String channelId, String message, String imageUrl) {
        String apiUrl = String.format(TELEGRAM_API_URL, botToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", channelId);
        requestBody.put("text", message);
        requestBody.put("parse_mode", "HTML");

        if (imageUrl != null && !imageUrl.isBlank()) {
            Map<String, Object> linkPreview = new HashMap<>();
            linkPreview.put("url", imageUrl);
            linkPreview.put("show_above_text", true);
            requestBody.put("link_preview_options", linkPreview);
        } else {
            requestBody.put("disable_web_page_preview", false);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                if (responseJson.get("ok").asBoolean()) {
                    String messageId = responseJson.get("result").get("message_id").asText();
                    log.info("Successfully sent message to Telegram. Message ID: {}", messageId);
                    return messageId;
                }
            }

            log.error("Telegram API returned error: {}", response.getBody());
            throw new RuntimeException("Telegram API error: " + response.getBody());
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            throw new RuntimeException("Telegram send failed: " + e.getMessage(), e);
        }
    }

    /**
     * Format the message using template or default format.
     */
    private String formatMessage(String title, String content, String url, String template) {
        // If template custom logic implemented, user would need {{content}}
        // placeholder.
        // For now, override with default improved format.

        StringBuilder sb = new StringBuilder();

        // 1. Title
        sb.append("📰 <b>").append(escapeHtml(title)).append("</b>\n\n");

        // 2. Content (Truncated to fit)
        // Max limit adjusted to 4000 to maximize content (Telegram limit 4096)
        if (content != null && !content.isBlank()) {
            int maxContent = 4000;
            String cleanContent = content.length() > maxContent ? content.substring(0, maxContent) + "..." : content;
            sb.append(escapeHtml(cleanContent)).append("\n\n");
        }

        // 3. Footer Link
        sb.append("🔗 <a href=\"").append(url).append("\">Read Full Article</a>");

        return sb.toString();
    }

    /**
     * Escape special HTML characters for Telegram HTML parse mode.
     */
    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Test the Telegram connection with current configuration.
     */
    public boolean testConnection() {
        Optional<SocialPlatformConfig> configOpt = configRepository
                .findByPlatform(SocialPlatformConfig.Platform.TELEGRAM);

        if (configOpt.isEmpty()) {
            return false;
        }

        try {
            JsonNode credentials = objectMapper.readTree(configOpt.get().getCredentials());
            String botToken = credentials.get("botToken").asText();

            // Test with getMe API call
            String testUrl = "https://api.telegram.org/bot" + botToken + "/getMe";
            ResponseEntity<String> response = restTemplate.getForEntity(testUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode result = objectMapper.readTree(response.getBody());
                return result.get("ok").asBoolean();
            }
            return false;
        } catch (Exception e) {
            log.error("Telegram connection test failed", e);
            return false;
        }
    }
}
