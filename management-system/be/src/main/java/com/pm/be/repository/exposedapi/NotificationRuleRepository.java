package com.pm.be.repository.exposedapi;

import com.pm.be.entity.exposedapi.NotificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, UUID> {
}
