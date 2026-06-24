package com.pm.be.dto.response.client;

import com.pm.be.enums.ClientStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientResponse {
    UUID id;
    String name;
    String clientCode;
    String description;
    String email;
    ClientStatus status;
    LocalDateTime revokedAt;
    String revokedBy;
    String revokeReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
