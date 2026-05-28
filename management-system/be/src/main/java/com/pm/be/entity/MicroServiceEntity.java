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
@Table(name = "micro_service")
public class MicroServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false, length = 100)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "service_url", length = 255)
    String serviceUrl;

    @Column(name = "key_service", unique = true)
    String keyService;

    @Column(length = 20)
    @Builder.Default
    String status = "ACTIVE";

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
