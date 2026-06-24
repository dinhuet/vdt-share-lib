package com.pm.be.dto.request.clientpermission;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientPermissionGrantRequest {
    UUID exposedApiId;
}
