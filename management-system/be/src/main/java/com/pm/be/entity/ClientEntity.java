package com.pm.be.entity;

import com.pm.be.enums.ClientStatus;
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
@Table(name = "client")
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "client_code", unique = true, nullable = false, length = 255)
    String clientCode;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(length = 255)
    String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    ClientStatus status = ClientStatus.ACTIVE;

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
