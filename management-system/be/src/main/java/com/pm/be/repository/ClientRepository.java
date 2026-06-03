package com.pm.be.repository;

import com.pm.be.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID>, JpaSpecificationExecutor<ClientEntity> {
    Optional<ClientEntity> findByClientCode(String clientCode);

    boolean existsByClientCode(String clientCode);
}
