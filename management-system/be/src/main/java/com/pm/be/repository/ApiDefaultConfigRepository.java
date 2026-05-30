package com.pm.be.repository;

import com.pm.be.entity.ApiDefaultConfigEntity;
import com.pm.be.enums.DefaultConfigScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiDefaultConfigRepository extends JpaRepository<ApiDefaultConfigEntity, UUID> {
    Optional<ApiDefaultConfigEntity> findByScopeAndMicroServiceId(DefaultConfigScope scope, UUID microServiceId);

    Optional<ApiDefaultConfigEntity> findByScopeAndMicroServiceIdIsNull(DefaultConfigScope scope);
}
