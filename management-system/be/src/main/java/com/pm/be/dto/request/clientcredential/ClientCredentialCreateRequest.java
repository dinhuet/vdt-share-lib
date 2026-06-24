package com.pm.be.dto.request.clientcredential;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCredentialCreateRequest {
    UUID microServiceId;
    String keyId;
    LocalDateTime expiresAt;
}
