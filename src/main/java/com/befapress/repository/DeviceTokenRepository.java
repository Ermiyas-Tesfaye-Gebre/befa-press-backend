package com.befapress.repository;

import com.befapress.entity.DeviceToken;
import com.befapress.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    // Find by token
    Optional<DeviceToken> findByToken(String token);

    // Find by user and token
    Optional<DeviceToken> findByUserAndToken(User user, String token);

    // Check if token exists
    boolean existsByToken(String token);

    // Get all active tokens for a user
    List<DeviceToken> findByUserAndIsActiveTrue(User user);

    // Get all active tokens for a list of users
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.user.id IN :userIds AND dt.isActive = true")
    List<DeviceToken> findActiveTokensByUserIds(@Param("userIds") List<Long> userIds);

    // Get all active tokens (for broadcast notifications)
    List<DeviceToken> findByIsActiveTrue();

    // Deactivate token
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.token = :token")
    void deactivateToken(@Param("token") String token);

    // Deactivate all tokens for user
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.user = :user")
    void deactivateAllTokensForUser(@Param("user") User user);

    // Delete by token
    void deleteByToken(String token);

    // Count active tokens for user
    long countByUserAndIsActiveTrue(User user);

    // Update last used timestamp
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.lastUsedAt = CURRENT_TIMESTAMP WHERE dt.token = :token")
    void updateLastUsed(@Param("token") String token);
}
