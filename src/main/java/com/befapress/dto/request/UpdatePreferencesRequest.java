package com.befapress.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {

    // Notification preferences
    private Boolean pushEnabled;
    private Boolean notifyBreakingNews;
    private Boolean notifyNewOpinions;
    private Boolean notifyComments;
    private Boolean notifyLikes;
    private Boolean emailNotifications;

    // Display preferences
    private String language;
    private String fontSize;
    private Boolean darkMode;

    // Content preferences
    private List<Long> preferredCategoryIds;
}
