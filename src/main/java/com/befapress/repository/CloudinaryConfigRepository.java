package com.befapress.repository;

import com.befapress.entity.CloudinaryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CloudinaryConfigRepository extends JpaRepository<CloudinaryConfig, Long> {

    /**
     * Get the single configuration row (first by ID).
     * This follows the single-row pattern — there should only be one config row.
     */
    Optional<CloudinaryConfig> findFirstByOrderByIdAsc();
}
