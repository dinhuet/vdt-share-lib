package com.pm.be.repository;

import com.pm.be.entity.AnomalyBaselineRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnomalyBaselineRuleConfigRepository extends JpaRepository<AnomalyBaselineRuleConfigEntity, UUID> {
}
