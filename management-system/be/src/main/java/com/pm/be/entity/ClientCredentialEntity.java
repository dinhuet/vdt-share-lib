package com.pm.be.entity;

import com.pm.be.enums.ClientCredentialStatus;
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
@Table(name = "client_credential", uniqueConstraints = {
        @UniqueConstraint(name = "uk_client_credential_key_id", columnNames = {"key_id"}),
        @UniqueConstraint(name = "uk_client_credential_api_key_hash", columnNames = {"api_key_hash"})
})
public class ClientCredentialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "client_id", nullable = false)
    UUID clientId;

    @Column(name = "micro_service_id", nullable = false)
    UUID microServiceId;

    @Column(name = "key_id", nullable = false, length = 100)
    String keyId;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    String apiKeyHash;

    @Column(name = "signing_secret_encrypted", nullable = false, columnDefinition = "TEXT")
    String signingSecretEncrypted;

    @Column(nullable = false, length = 50)
    @Builder.Default
    String algorithm = "HMAC-SHA256";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    ClientCredentialStatus status = ClientCredentialStatus.ACTIVE;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    LocalDateTime revokedAt;

    @Column(name = "revoked_by", length = 255)
    String revokedBy;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    String revokeReason;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
