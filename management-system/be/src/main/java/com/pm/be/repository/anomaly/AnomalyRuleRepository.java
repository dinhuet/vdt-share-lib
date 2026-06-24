package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.AnomalyRuleEntity;
import com.pm.be.enums.AnomalyRuleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnomalyRuleRepository extends JpaRepository<AnomalyRuleEntity, UUID> {
    Optional<AnomalyRuleEntity> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);

    @EntityGraph(attributePaths = "staticConfig")
    List<AnomalyRuleEntity> findByEnabledTrueAndRuleType(AnomalyRuleType ruleType);

    @EntityGraph(attributePaths = "baselineConfig")
    @Query("select r from AnomalyRuleEntity r where r.enabled = true and r.ruleType in :ruleTypes")
    List<AnomalyRuleEntity> findEnabledBaselineRules(List<AnomalyRuleType> ruleTypes);
}
