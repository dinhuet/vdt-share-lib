package com.pm.be.repository;

import com.pm.be.entity.ClientCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientCredentialRepository extends JpaRepository<ClientCredentialEntity, UUID> {
    Optional<ClientCredentialEntity> findByClientIdAndMicroServiceIdAndKeyId(UUID clientId, UUID microServiceId, String keyId);

    List<ClientCredentialEntity> findByClientId(UUID clientId);

    List<ClientCredentialEntity> findByClientIdAndMicroServiceId(UUID clientId, UUID microServiceId);
}
