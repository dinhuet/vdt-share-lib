package com.pm.sharedlib.endpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class EndpointManifest {
    @Builder.Default
    int version = 1;
    String serviceName;
    String generatedAt;
    @Builder.Default
    List<EndpointDefinition> exposedApis = new ArrayList<>();
    @Builder.Default
    List<EndpointDefinition> clientApis = new ArrayList<>();
}
