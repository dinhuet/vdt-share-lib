package com.pm.be.dto.request.accesspolicy;

import com.pm.be.enums.AccessPolicyMatchType;
import com.pm.be.enums.AccessPolicyType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessPolicyUpsertRequest {
    AccessPolicyType type;
    AccessPolicyMatchType matchType;
    String matchValue;
    Boolean temporary;
    LocalDateTime expiresAt;
}
