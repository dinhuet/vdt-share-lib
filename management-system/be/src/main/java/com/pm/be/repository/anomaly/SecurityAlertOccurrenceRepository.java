package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertOccurrenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecurityAlertOccurrenceRepository extends JpaRepository<SecurityAlertOccurrenceEntity, UUID> {
    List<SecurityAlertOccurrenceEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId);
}
