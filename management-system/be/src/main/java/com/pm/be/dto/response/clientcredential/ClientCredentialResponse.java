package com.pm.be.dto.response.clientcredential;

import com.pm.be.enums.ClientCredentialStatus;
import com.pm.be.enums.CredentialExpiryState;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCredentialResponse {
    UUID id;
    UUID clientId;
    String clientCode;
    UUID microServiceId;
    String microServiceName;
    String keyId;
    String algorithm;
    ClientCredentialStatus status;
    LocalDateTime expiresAt;
    CredentialExpiryState expiryState;
    Long daysUntilExpiry;
    LocalDateTime revokedAt;
    String revokedBy;
    String revokeReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
