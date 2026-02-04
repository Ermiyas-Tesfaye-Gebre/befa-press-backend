package com.befapress.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for configuring social media platforms.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialConfigRequest {

    // Telegram specific
    private String botToken;
    private String channelId;

    // Facebook specific
    private String pageId;
    private String accessToken;

    // Twitter/X specific
    private String apiKey;
    private String apiSecret;
    private String accessTokenKey;
    private String accessTokenSecret;

    // Common fields
    private String channelUrl;
    private Boolean enabled;
    private Boolean shareNews;
    private Boolean shareOpinions;
    private String messageTemplate;
}
