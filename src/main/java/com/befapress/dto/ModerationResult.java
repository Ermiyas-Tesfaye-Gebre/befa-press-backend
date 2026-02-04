package com.befapress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of content moderation analysis.
 * Contains whether content is harmful and detected violation categories.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    /**
     * Whether the content contains harmful material
     */
    @Builder.Default
    private boolean harmful = false;

    /**
     * List of detected violation categories
     * e.g., INSULT, HATE_SPEECH, THREAT, MOCKERY, AD_HOMINEM, OFF_TOPIC
     */
    @Builder.Default
    private List<String> detectedCategories = new ArrayList<>();

    /**
     * User-facing rejection reason message
     */
    private String reason;

    /**
     * Matched harmful words/phrases for logging/debugging
     */
    @Builder.Default
    private List<String> matchedTerms = new ArrayList<>();

    /**
     * Confidence score (0.0 - 1.0) based on number of matches
     */
    @Builder.Default
    private double confidenceScore = 0.0;

    /**
     * Quick check if content passed moderation
     */
    public boolean isPassed() {
        return !harmful;
    }

    /**
     * Get formatted categories as comma-separated string
     */
    public String getCategoriesAsString() {
        return String.join(", ", detectedCategories);
    }

    /**
     * Create a passing result (no violations)
     */
    public static ModerationResult passed() {
        return ModerationResult.builder()
                .harmful(false)
                .build();
    }

    /**
     * Create a failing result with categories
     */
    public static ModerationResult failed(List<String> categories, String reason) {
        return ModerationResult.builder()
                .harmful(true)
                .detectedCategories(categories)
                .reason(reason)
                .build();
    }
}
