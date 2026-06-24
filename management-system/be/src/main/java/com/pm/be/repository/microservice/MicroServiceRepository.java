package com.pm.be.repository.microservice;

import com.pm.be.entity.microservice.MicroServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MicroServiceRepository extends JpaRepository<MicroServiceEntity, UUID> {
    Optional<MicroServiceEntity> findByName(String name);
}
