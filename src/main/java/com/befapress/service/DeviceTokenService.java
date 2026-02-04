package com.befapress.service;

import com.befapress.entity.DeviceToken;
import com.befapress.entity.User;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.DeviceTokenRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * Register a new device token for push notifications
     */
    @Transactional
    public void registerToken(String userEmail, String token, String platform, String deviceName) {
        User user = getUserByEmail(userEmail);

        // Check if token already exists for this user
        if (deviceTokenRepository.findByUserAndToken(user, token).isPresent()) {
            // Token exists, just update last used
            deviceTokenRepository.updateLastUsed(token);
            return;
        }

        // Check if token exists for another user (device switched accounts)
        deviceTokenRepository.findByToken(token).ifPresent(existingToken -> {
            existingToken.setActive(false);
            deviceTokenRepository.save(existingToken);
        });

        // Create new token
        DeviceToken deviceToken = DeviceToken.builder()
                .user(user)
                .token(token)
                .platform(platform != null ? platform.toUpperCase() : "UNKNOWN")
                .deviceName(deviceName)
                .isActive(true)
                .build();

        deviceTokenRepository.save(deviceToken);
        log.info("Registered FCM token for user: {}", userEmail);
    }

    /**
     * Unregister a device token
     */
    @Transactional
    public void unregisterToken(String token) {
        deviceTokenRepository.deactivateToken(token);
        log.info("Deactivated FCM token: {}...", token.substring(0, Math.min(20, token.length())));
    }

    /**
     * Unregister all tokens for a user (e.g., on logout from all devices)
     */
    @Transactional
    public void unregisterAllTokensForUser(String userEmail) {
        User user = getUserByEmail(userEmail);
        deviceTokenRepository.deactivateAllTokensForUser(user);
        log.info("Deactivated all FCM tokens for user: {}", userEmail);
    }

    /**
     * Get all active tokens for a user
     */
    public List<String> getActiveTokensForUser(String userEmail) {
        User user = getUserByEmail(userEmail);
        return deviceTokenRepository.findByUserAndIsActiveTrue(user).stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    /**
     * Get all active tokens for a list of user IDs (for batch notifications)
     */
    public List<String> getActiveTokensForUsers(List<Long> userIds) {
        return deviceTokenRepository.findActiveTokensByUserIds(userIds).stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    /**
     * Get all active tokens (for broadcast notifications)
     */
    public List<String> getAllActiveTokens() {
        return deviceTokenRepository.findByIsActiveTrue().stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    /**
     * Update last used timestamp for a token
     */
    @Transactional
    public void updateLastUsed(String token) {
        deviceTokenRepository.updateLastUsed(token);
    }

    /**
     * Mark token as invalid (e.g., FCM returned error)
     */
    @Transactional
    public void markTokenAsInvalid(String token) {
        deviceTokenRepository.deactivateToken(token);
        log.warn("Marked FCM token as invalid: {}...", token.substring(0, Math.min(20, token.length())));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
