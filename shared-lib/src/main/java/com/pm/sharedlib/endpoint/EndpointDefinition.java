package com.pm.sharedlib.endpoint;

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
public class EndpointDefinition {
    UUID endpointId;
    String endpointKey;
    EndpointType type;
    String protocol;
    String name;
    String method;
    String path;
    String destinationUrl;
    String topic;
    String handlerClass;
    String handlerMethod;
}
