package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecurityAlertActionRepository extends JpaRepository<SecurityAlertActionEntity, UUID> {
    List<SecurityAlertActionEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId);

    void deleteByAlertId(UUID alertId);
}
