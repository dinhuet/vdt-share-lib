package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyOccurrenceProperties;
import com.pm.be.dto.anomaly.AnomalyRuleMatch;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.repository.anomaly.SecurityAlertOccurrenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAlertOccurrenceServiceTest {
    @Mock SecurityAlertOccurrenceRepository repository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    private SecurityAlertOccurrenceService service;

    @BeforeEach
    void setUp() {
        service = new SecurityAlertOccurrenceService(repository, redisTemplate, new AnomalyOccurrenceProperties());
    }

    @Test
    void createIfAllowed_noCooldown_shouldCreateOccurrence() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("vdt:anomaly:occurrence-cooldown:fp:2026-06-23T10:00:00Z"), eq("1"), eq(Duration.ofSeconds(120))))
                .thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<?> occurrence = service.createIfAllowed(alert(), match());

        assertThat(occurrence).isPresent();
    }

    @Test
    void createIfAllowed_duplicateCooldown_shouldSkipOccurrence() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("vdt:anomaly:occurrence-cooldown:fp:2026-06-23T10:00:00Z"), eq("1"), eq(Duration.ofSeconds(120))))
                .thenReturn(false);

        Optional<?> occurrence = service.createIfAllowed(alert(), match());

        assertThat(occurrence).isEmpty();
        verify(repository, never()).save(any());
    }

    private SecurityAlertEntity alert() {
        return SecurityAlertEntity.builder()
                .id(UUID.randomUUID())
                .alertType("AUTH_BRUTE_FORCE")
                .severity(AnomalySeverity.HIGH)
                .status(SecurityAlertStatus.OPEN)
                .fingerprint("fp")
                .metric("auth_fail_count")
                .scopeType(AnomalyScopeType.ENDPOINT_CLIENT)
                .currentValue(BigDecimal.TEN)
                .thresholdValue(BigDecimal.ONE)
                .windowSeconds(60)
                .count(10L)
                .build();
    }

    private AnomalyRuleMatch match() {
        SecurityLogEventMessage event = new SecurityLogEventMessage();
        event.setTimestamp(Instant.parse("2026-06-23T10:00:01Z"));
        return new AnomalyRuleMatch(null, "AUTH_BRUTE_FORCE", "STATIC", AnomalySeverity.HIGH, "auth_fail_count",
                AnomalyScopeType.ENDPOINT_CLIENT, "scope", "client-a", BigDecimal.TEN, null, BigDecimal.ONE, 60,
                null, "AUTH_BRUTE_FORCE", null, null, null, Instant.parse("2026-06-23T10:00:00Z"),
                Instant.parse("2026-06-23T10:01:00Z"), event);
    }
}
