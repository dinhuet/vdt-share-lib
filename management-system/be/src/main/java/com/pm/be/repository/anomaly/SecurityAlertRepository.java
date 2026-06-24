package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.SecurityAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlertEntity, UUID> {
    Optional<SecurityAlertEntity> findFirstByFingerprintAndStatusIn(String fingerprint, Collection<SecurityAlertStatus> statuses);
}
