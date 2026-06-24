package com.pm.be.repository;

import com.pm.be.entity.AnomalyRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnomalyRuleRepository extends JpaRepository<AnomalyRuleEntity, UUID> {
    Optional<AnomalyRuleEntity> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);
}
