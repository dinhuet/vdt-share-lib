package com.pm.be.entity;

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
@Table(name = "auth_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id", "exposed_api_id"})
})
public class AuthConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "client_id", nullable = false)
    UUID clientId;

    @Column(name = "exposed_api_id")
    UUID exposedApiId;

    @Column(nullable = false, length = 20)
    String type;

    @Column(name = "api_secret", length = 255)
    String apiSecret;

    @Column(name = "public_key", columnDefinition = "TEXT")
    String publicKey;

    @Column(length = 50)
    String algorithm;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "is_enabled")
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
