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
@Table(name = "notification_rule")
public class NotificationRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(nullable = false, columnDefinition = "jsonb")
    String recipients;

    @Column(length = 20)
    @Builder.Default
    String severity = "WARNING";

    @Column(name = "cooldown_minutes")
    @Builder.Default
    Integer cooldownMinutes = 5;

    @Column(name = "is_enabled")
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
