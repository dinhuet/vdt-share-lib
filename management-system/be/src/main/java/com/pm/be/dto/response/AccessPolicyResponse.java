package com.pm.be.dto.response;

import com.pm.be.enums.AccessPolicyMatchType;
import com.pm.be.enums.AccessPolicyType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessPolicyResponse {
    UUID id;
    UUID exposedApiId;
    AccessPolicyType type;
    AccessPolicyMatchType matchType;
    String matchValue;
    Boolean temporary;
    LocalDateTime expiresAt;
    String createdBy;
    LocalDateTime createdAt;
}
