package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.AnomalyBaselineRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnomalyBaselineRuleConfigRepository extends JpaRepository<AnomalyBaselineRuleConfigEntity, UUID> {
}
