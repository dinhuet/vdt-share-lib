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
@Table(name = "client_exposed_api_permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_client_exposed_api_permission", columnNames = {"client_id", "exposed_api_id"})
})
public class ClientExposedApiPermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "client_id", nullable = false)
    UUID clientId;

    @Column(name = "exposed_api_id", nullable = false)
    UUID exposedApiId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
