package com.pm.sharedlib.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ClientPermissionRuntimeConfig {
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
    String createdAt;
    String updatedAt;
}
