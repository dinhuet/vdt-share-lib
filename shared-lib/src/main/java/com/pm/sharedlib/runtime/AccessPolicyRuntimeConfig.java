package com.pm.sharedlib.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AccessPolicyRuntimeConfig {
    UUID id;
    UUID exposedApiId;
    String type;
    String matchType;
    String matchValue;
    Boolean temporary;
    LocalDateTime expiresAt;
    String createdBy;
    LocalDateTime createdAt;
}
