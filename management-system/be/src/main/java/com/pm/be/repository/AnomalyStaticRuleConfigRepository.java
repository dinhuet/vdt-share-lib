package com.pm.be.repository;

import com.pm.be.entity.AnomalyStaticRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnomalyStaticRuleConfigRepository extends JpaRepository<AnomalyStaticRuleConfigEntity, UUID> {
}
