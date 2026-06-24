package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.AnomalyStaticRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnomalyStaticRuleConfigRepository extends JpaRepository<AnomalyStaticRuleConfigEntity, UUID> {
}
