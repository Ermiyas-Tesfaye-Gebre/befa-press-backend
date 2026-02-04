package com.befapress.service;

import com.befapress.dto.request.UpdatePreferencesRequest;
import com.befapress.dto.response.UserPreferencesResponse;
import com.befapress.entity.User;
import com.befapress.entity.UserPreferences;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.UserPreferencesRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    /**
     * Get user preferences (creates default if not exists)
     */
    @Transactional
    public UserPreferencesResponse getPreferences(String userEmail) {
        User user = getUserByEmail(userEmail);
        UserPreferences prefs = preferencesRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));
        return mapToResponse(prefs);
    }

    /**
     * Update user preferences
     */
    @Transactional
    public UserPreferencesResponse updatePreferences(String userEmail, UpdatePreferencesRequest request) {
        User user = getUserByEmail(userEmail);
        UserPreferences prefs = preferencesRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));

        // Update notification preferences
        if (request.getPushEnabled() != null) {
            prefs.setPushEnabled(request.getPushEnabled());
        }
        if (request.getNotifyBreakingNews() != null) {
            prefs.setNotifyBreakingNews(request.getNotifyBreakingNews());
        }
        if (request.getNotifyNewOpinions() != null) {
            prefs.setNotifyNewOpinions(request.getNotifyNewOpinions());
        }
        if (request.getNotifyComments() != null) {
            prefs.setNotifyComments(request.getNotifyComments());
        }
        if (request.getNotifyLikes() != null) {
            prefs.setNotifyLikes(request.getNotifyLikes());
        }
        if (request.getEmailNotifications() != null) {
            prefs.setEmailNotifications(request.getEmailNotifications());
        }

        // Update display preferences
        if (request.getLanguage() != null) {
            prefs.setLanguage(request.getLanguage());
        }
        if (request.getFontSize() != null) {
            prefs.setFontSize(request.getFontSize());
        }
        if (request.getDarkMode() != null) {
            prefs.setDarkMode(request.getDarkMode());
        }

        // Update content preferences
        if (request.getPreferredCategoryIds() != null) {
            String categoryIds = request.getPreferredCategoryIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            prefs.setPreferredCategories(categoryIds);
        }

        preferencesRepository.save(prefs);
        return mapToResponse(prefs);
    }

    private UserPreferences createDefaultPreferences(User user) {
        UserPreferences prefs = UserPreferences.builder()
                .user(user)
                .pushEnabled(true)
                .notifyBreakingNews(true)
                .notifyNewOpinions(false)
                .notifyComments(true)
                .notifyLikes(false)
                .emailNotifications(true)
                .language("en")
                .fontSize("medium")
                .darkMode(false)
                .build();
        return preferencesRepository.save(prefs);
    }

    private UserPreferencesResponse mapToResponse(UserPreferences prefs) {
        List<Long> categoryIds = null;
        if (prefs.getPreferredCategories() != null && !prefs.getPreferredCategories().isEmpty()) {
            categoryIds = Arrays.stream(prefs.getPreferredCategories().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        return UserPreferencesResponse.builder()
                .pushEnabled(prefs.isPushEnabled())
                .notifyBreakingNews(prefs.isNotifyBreakingNews())
                .notifyNewOpinions(prefs.isNotifyNewOpinions())
                .notifyComments(prefs.isNotifyComments())
                .notifyLikes(prefs.isNotifyLikes())
                .emailNotifications(prefs.isEmailNotifications())
                .language(prefs.getLanguage())
                .fontSize(prefs.getFontSize())
                .darkMode(prefs.isDarkMode())
                .preferredCategoryIds(categoryIds)
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
