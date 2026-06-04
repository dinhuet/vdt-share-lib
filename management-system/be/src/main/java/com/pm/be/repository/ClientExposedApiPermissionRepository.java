package com.pm.be.repository;

import com.pm.be.entity.ClientExposedApiPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientExposedApiPermissionRepository extends JpaRepository<ClientExposedApiPermissionEntity, UUID> {
    Optional<ClientExposedApiPermissionEntity> findByClientIdAndExposedApiId(UUID clientId, UUID exposedApiId);

    List<ClientExposedApiPermissionEntity> findByClientId(UUID clientId);

    List<ClientExposedApiPermissionEntity> findByExposedApiId(UUID exposedApiId);

    boolean existsByClientIdAndExposedApiId(UUID clientId, UUID exposedApiId);

    boolean existsByClientIdAndExposedApiIdAndEnabled(UUID clientId, UUID exposedApiId, Boolean enabled);
}
