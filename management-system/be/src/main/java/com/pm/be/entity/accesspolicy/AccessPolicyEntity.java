package com.pm.be.entity.accesspolicy;

import com.pm.be.enums.AccessPolicyMatchType;
import com.pm.be.enums.AccessPolicyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "access_policy", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exposed_api_id", "match_type", "match_value"})
})
public class AccessPolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "exposed_api_id", nullable = false)
    UUID exposedApiId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AccessPolicyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    AccessPolicyMatchType matchType;

    @Column(name = "match_value", nullable = false, length = 255)
    String matchValue;

    @Column(name = "is_temporary")
    @Builder.Default
    Boolean temporary = false;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "created_by", length = 100)
    String createdBy;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
