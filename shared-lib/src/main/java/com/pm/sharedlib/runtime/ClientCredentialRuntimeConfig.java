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
public class ClientCredentialRuntimeConfig {
    UUID id;
    UUID clientId;
    String clientCode;
    String clientName;
    UUID microServiceId;
    String microServiceName;
    String keyId;
    String apiKeyHash;
    String signingSecretEncrypted;
    String algorithm;
    String status;
    LocalDateTime expiresAt;
    LocalDateTime revokedAt;
    String revokedBy;
    String revokeReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
