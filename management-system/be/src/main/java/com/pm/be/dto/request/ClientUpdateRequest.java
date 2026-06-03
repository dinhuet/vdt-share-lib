package com.pm.be.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientUpdateRequest {
    String name;
    String clientCode;
    String description;
    String email;
}
