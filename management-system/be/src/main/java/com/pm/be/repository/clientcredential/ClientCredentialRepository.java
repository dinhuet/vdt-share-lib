package com.pm.be.repository.clientcredential;

import com.pm.be.entity.clientcredential.ClientCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientCredentialRepository extends JpaRepository<ClientCredentialEntity, UUID> {
    List<ClientCredentialEntity> findByClientId(UUID clientId);

    List<ClientCredentialEntity> findByClientIdAndMicroServiceId(UUID clientId, UUID microServiceId);

    boolean existsByKeyId(String keyId);
}
