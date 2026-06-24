package com.pm.be.dto.response.clientcredential;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCredentialCreatedResponse extends ClientCredentialResponse {
    String apiKey;
    String signingSecret;

    @Builder(builderMethodName = "createdBuilder")
    public ClientCredentialCreatedResponse(ClientCredentialResponse response, String apiKey, String signingSecret) {
        super(response.getId(), response.getClientId(), response.getClientCode(), response.getMicroServiceId(),
                response.getMicroServiceName(), response.getKeyId(), response.getAlgorithm(), response.getStatus(),
                response.getExpiresAt(), response.getExpiryState(), response.getDaysUntilExpiry(), response.getRevokedAt(),
                response.getRevokedBy(), response.getRevokeReason(), response.getCreatedAt(), response.getUpdatedAt());
        this.apiKey = apiKey;
        this.signingSecret = signingSecret;
    }
}
