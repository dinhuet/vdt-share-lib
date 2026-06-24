package com.pm.be.dto.request.clientcredential;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCredentialRevokeRequest {
    String reason;
}
