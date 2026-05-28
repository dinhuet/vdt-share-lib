package com.pm.be.repository;

import com.pm.be.entity.MicroServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MicroServiceRepository extends JpaRepository<MicroServiceEntity, UUID> {
    Optional<MicroServiceEntity> findByName(String name);
    Optional<MicroServiceEntity> findByKeyService(String keyService);
}
