package com.befapress.repository;

import com.befapress.entity.SocialPlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialPlatformConfigRepository extends JpaRepository<SocialPlatformConfig, Long> {

    Optional<SocialPlatformConfig> findByPlatform(SocialPlatformConfig.Platform platform);

    List<SocialPlatformConfig> findByEnabledTrue();

    List<SocialPlatformConfig> findByEnabledTrueAndShareNewsTrue();

    List<SocialPlatformConfig> findByEnabledTrueAndShareOpinionsTrue();

    boolean existsByPlatform(SocialPlatformConfig.Platform platform);
}
