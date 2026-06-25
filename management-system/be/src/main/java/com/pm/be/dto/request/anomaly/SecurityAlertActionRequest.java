package com.pm.be.dto.request.anomaly;

import lombok.Data;

import java.time.Instant;

@Data
public class SecurityAlertActionRequest {
    private String reason;
    private Instant ignoredUntil;
}
