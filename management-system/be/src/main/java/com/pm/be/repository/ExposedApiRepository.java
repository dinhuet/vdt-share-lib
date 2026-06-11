package com.pm.be.repository;

import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExposedApiRepository extends JpaRepository<ExposedApiEntity, UUID> {
    Optional<ExposedApiEntity> findByMicroServiceIdAndName(UUID microServiceId, String name);

    Optional<ExposedApiEntity> findByMicroServiceIdAndEndpointId(UUID microServiceId, UUID endpointId);

    List<ExposedApiEntity> findByMicroServiceId(UUID microServiceId);

    List<ExposedApiEntity> findByMicroServiceIdAndUseDefaultConfig(UUID microServiceId, Boolean useDefaultConfig);

    List<ExposedApiEntity> findByUseDefaultConfig(Boolean useDefaultConfig);

    List<ExposedApiEntity> findBySyncStatus(SyncStatus syncStatus);
}
