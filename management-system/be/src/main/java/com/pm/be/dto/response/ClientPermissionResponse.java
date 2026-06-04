package com.pm.be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientPermissionResponse {
    UUID id;
    UUID clientId;
    String clientCode;
    String clientName;
    UUID exposedApiId;
    String exposedApiName;
    UUID microServiceId;
    String microServiceName;
    String method;
    String path;
    String protocol;
    Boolean enabled;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
