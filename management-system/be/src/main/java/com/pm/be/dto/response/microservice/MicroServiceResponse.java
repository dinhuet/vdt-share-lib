package com.pm.be.dto.response.microservice;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MicroServiceResponse {
    UUID id;
    String name;
    String description;
    String serviceUrl;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
