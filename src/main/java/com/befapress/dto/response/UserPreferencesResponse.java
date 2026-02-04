package com.befapress.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesResponse {

    // Notification preferences
    private boolean pushEnabled;
    private boolean notifyBreakingNews;
    private boolean notifyNewOpinions;
    private boolean notifyComments;
    private boolean notifyLikes;
    private boolean emailNotifications;

    // Display preferences
    private String language;
    private String fontSize;
    private boolean darkMode;

    // Content preferences
    private List<Long> preferredCategoryIds;
}
