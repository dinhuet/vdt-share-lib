package com.pm.be.service.anomaly;

import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.dto.anomaly.StaticRuleMatch;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAlertServiceTest {
    @Mock SecurityAlertRepository repository;
    @Mock SecurityAlertOccurrenceService occurrenceService;
    @Mock NotificationDispatcher notificationDispatcher;

    private SecurityAlertService service;

    @BeforeEach
    void setUp() {
        service = new SecurityAlertService(repository, new StaticRuleProperties(), occurrenceService, notificationDispatcher);
    }

    @Test
    void createOrUpdate_noExistingAlert_shouldCreateOpenAlert() {
        StaticRuleMatch match = match(BigDecimal.valueOf(5));
        when(repository.findFirstByFingerprintAndStatusIn(eq("AUTH_BRUTE_FORCE:INBOUND_HTTP:endpoint-1:ENDPOINT_CLIENT:client-a"), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAnomalyEvent event = service.createOrUpdate(match);

        assertThat(event.getFingerprint()).isEqualTo("AUTH_BRUTE_FORCE:INBOUND_HTTP:endpoint-1:ENDPOINT_CLIENT:client-a");
        assertThat(event.getStatus()).isEqualTo(SecurityAlertStatus.OPEN);
    }

    @Test
    void createOrUpdate_existingOpenAlert_shouldUpdateSameAlert() {
        StaticRuleMatch match = match(BigDecimal.valueOf(6));
        SecurityAlertEntity existing = SecurityAlertEntity.builder()
                .fingerprint("AUTH_BRUTE_FORCE:INBOUND_HTTP:endpoint-1:ENDPOINT_CLIENT:client-a")
                .severity(AnomalySeverity.MEDIUM)
                .status(SecurityAlertStatus.OPEN)
                .currentValue(BigDecimal.valueOf(5))
                .build();
        when(repository.findFirstByFingerprintAndStatusIn(eq(existing.getFingerprint()), any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAnomalyEvent event = service.createOrUpdate(match);

        assertThat(event.getCurrentValue()).isEqualByComparingTo("6");
        assertThat(event.getSeverity()).isEqualTo(AnomalySeverity.HIGH);
    }

    private StaticRuleMatch match(BigDecimal currentValue) {
        SecurityLogEventMessage event = new SecurityLogEventMessage();
        event.setTimestamp(Instant.parse("2026-06-23T10:00:01Z"));
        event.setFlowType("INBOUND_HTTP");
        event.setEndpointId("endpoint-1");
        event.setEndpointName("create-order");
        event.setServiceName("order-service");
        event.setClientId("client-a");
        event.setSourceIp("10.0.0.1");
        event.setResultCode("AUTH_API_KEY_INVALID");
        return new StaticRuleMatch(null, "AUTH_BRUTE_FORCE", AnomalySeverity.HIGH, "auth_fail_count", AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:client-a", "client-a", currentValue, BigDecimal.valueOf(5), 60,
                Instant.parse("2026-06-23T10:00:00Z"), Instant.parse("2026-06-23T10:01:00Z"), null, event);
    }
}
