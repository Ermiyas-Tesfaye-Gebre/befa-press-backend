package com.befapress.repository;

import com.befapress.entity.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {

    /**
     * Get the single settings row (there should only ever be one)
     */
    default Optional<SiteSettings> getSettings() {
        return findById(1L);
    }
}
