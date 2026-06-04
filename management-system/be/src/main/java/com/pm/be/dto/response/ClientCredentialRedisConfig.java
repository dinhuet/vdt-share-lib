package com.pm.be.dto.response;

import com.pm.be.enums.ClientCredentialStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCredentialRedisConfig {
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
    ClientCredentialStatus status;
    LocalDateTime expiresAt;
    LocalDateTime revokedAt;
    String revokedBy;
    String revokeReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
