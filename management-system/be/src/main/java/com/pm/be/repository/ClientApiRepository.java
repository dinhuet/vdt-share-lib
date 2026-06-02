package com.pm.be.repository;

import com.pm.be.entity.ClientApiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ClientApiRepository extends JpaRepository<ClientApiEntity, UUID>, JpaSpecificationExecutor<ClientApiEntity> {
    Optional<ClientApiEntity> findByMicroServiceIdAndName(UUID microServiceId, String name);
}
