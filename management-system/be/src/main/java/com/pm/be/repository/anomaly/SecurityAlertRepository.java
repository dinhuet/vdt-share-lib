package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.SecurityAlertStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlertEntity, UUID>, JpaSpecificationExecutor<SecurityAlertEntity> {
    Optional<SecurityAlertEntity> findFirstByFingerprintAndStatusIn(String fingerprint, Collection<SecurityAlertStatus> statuses);

    long countByStatus(SecurityAlertStatus status);

    long countByStatusAndSeverity(SecurityAlertStatus status, com.pm.be.enums.AnomalySeverity severity);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    Optional<SecurityAlertEntity> findFirstByOrderByCreatedAtDesc();

    List<SecurityAlertEntity> findByOrderByCreatedAtDesc(Pageable pageable);
}
