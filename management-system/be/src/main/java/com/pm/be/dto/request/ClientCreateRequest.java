package com.pm.be.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientCreateRequest {
    String name;
    String clientCode;
    String description;
    String email;
}
