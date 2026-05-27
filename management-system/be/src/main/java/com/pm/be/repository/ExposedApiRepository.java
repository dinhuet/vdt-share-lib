package com.pm.be.repository;

import com.pm.be.entity.ExposedApiEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExposedApiRepository extends JpaRepository<ExposedApiEntity, UUID> {
    Optional<ExposedApiEntity> findByMicroServiceIdAndName(UUID microServiceId, String name);
}
