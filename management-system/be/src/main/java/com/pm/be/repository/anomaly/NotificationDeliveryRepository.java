package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, UUID> {
    List<NotificationDeliveryEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId);
}
