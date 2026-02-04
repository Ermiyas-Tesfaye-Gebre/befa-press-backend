package com.befapress.repository;

import com.befapress.entity.ModerationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationRuleRepository extends JpaRepository<ModerationRule, Long> {
    List<ModerationRule> findAllByLanguage(String language);
}
