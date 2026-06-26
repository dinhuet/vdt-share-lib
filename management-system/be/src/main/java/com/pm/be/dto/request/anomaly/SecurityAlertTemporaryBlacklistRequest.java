package com.pm.be.dto.request.anomaly;

import com.pm.be.enums.SecurityAlertActionTargetType;
import lombok.Data;

@Data
public class SecurityAlertTemporaryBlacklistRequest {
    private SecurityAlertActionTargetType targetType;
    private String targetValue;
    private Integer durationMinutes;
    private String reason;
}
