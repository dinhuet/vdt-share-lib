package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;

public record ScopeCandidate(AnomalyScopeType scopeType, String scopeKey, String identity) {
}
