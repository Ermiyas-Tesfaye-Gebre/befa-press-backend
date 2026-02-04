package com.befapress.repository;

import com.befapress.entity.User;
import com.befapress.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    // Find by user
    Optional<UserPreferences> findByUser(User user);

    // Check if preferences exist for user
    boolean existsByUser(User user);
}
