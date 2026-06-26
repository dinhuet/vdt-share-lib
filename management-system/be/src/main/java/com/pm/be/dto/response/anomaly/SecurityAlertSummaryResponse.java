package com.pm.be.dto.response.anomaly;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SecurityAlertSummaryResponse {
    private long openCount;
    private long mediumOpenCount;
    private long highOpenCount;
    private long criticalOpenCount;
    private long recent24hCount;
    private LocalDateTime latestAlertAt;
}
