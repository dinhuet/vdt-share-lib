package com.pm.be.repository;

import com.pm.be.entity.AccessPolicyEntity;
import com.pm.be.enums.AccessPolicyMatchType;
import com.pm.be.enums.AccessPolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessPolicyRepository extends JpaRepository<AccessPolicyEntity, UUID> {
    List<AccessPolicyEntity> findByExposedApiId(UUID exposedApiId);

    List<AccessPolicyEntity> findByExposedApiIdAndType(UUID exposedApiId, AccessPolicyType type);

    Optional<AccessPolicyEntity> findByExposedApiIdAndMatchTypeAndMatchValue(
            UUID exposedApiId,
            AccessPolicyMatchType matchType,
            String matchValue);

    boolean existsByExposedApiIdAndMatchTypeAndMatchValue(
            UUID exposedApiId,
            AccessPolicyMatchType matchType,
            String matchValue);
}
