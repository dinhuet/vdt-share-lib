package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyNotificationProperties;
import com.pm.be.dto.request.anomaly.SecurityAlertTemporaryBlacklistRequest;
import com.pm.be.entity.accesspolicy.AccessPolicyEntity;
import com.pm.be.entity.anomaly.SecurityAlertActionEntity;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.exposedapi.ExposedApiEntity;
import com.pm.be.enums.*;
import com.pm.be.repository.accesspolicy.AccessPolicyRepository;
import com.pm.be.repository.anomaly.SecurityAlertActionRepository;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
import com.pm.be.repository.exposedapi.ExposedApiRepository;
import com.pm.be.service.accesspolicy.AccessPolicyRedisSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAlertBlacklistServiceTest {
    @Mock SecurityAlertRepository alertRepository;
    @Mock SecurityAlertActionRepository actionRepository;
    @Mock AccessPolicyRepository accessPolicyRepository;
    @Mock ExposedApiRepository exposedApiRepository;
    @Mock AccessPolicyRedisSyncService redisSyncService;

    private AnomalyNotificationProperties properties;
    private SecurityAlertBlacklistService service;

    @BeforeEach
    void setUp() {
        properties = new AnomalyNotificationProperties();
        service = new SecurityAlertBlacklistService(alertRepository, actionRepository, accessPolicyRepository,
                exposedApiRepository, redisSyncService, properties);
    }

    @Test
    void autoBlacklistCritical_withClientTarget_shouldCreateBlackPolicyAndAudit() {
        UUID exposedApiId = UUID.randomUUID();
        SecurityAlertEntity alert = alert(exposedApiId.toString());
        alert.setSeverity(AnomalySeverity.CRITICAL);
        alert.setClientId("client-1");
        when(exposedApiRepository.existsById(exposedApiId)).thenReturn(true);
        when(accessPolicyRepository.findByExposedApiIdAndMatchTypeAndMatchValue(exposedApiId, AccessPolicyMatchType.CLIENT_ID, "client-1"))
                .thenReturn(Optional.empty());
        when(accessPolicyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AccessPolicyEntity> saved = service.autoBlacklistCritical(alert);

        assertThat(saved).isPresent();
        assertThat(saved.get().getType()).isEqualTo(AccessPolicyType.BLACK);
        assertThat(saved.get().getMatchType()).isEqualTo(AccessPolicyMatchType.CLIENT_ID);
        assertThat(saved.get().getTemporary()).isTrue();
        ArgumentCaptor<SecurityAlertActionEntity> actionCaptor = ArgumentCaptor.forClass(SecurityAlertActionEntity.class);
        verify(actionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(SecurityAlertActionType.TEMP_BLACKLIST);
        verify(redisSyncService).syncByExposedApiId(exposedApiId);
    }

    @Test
    void temporaryBlacklist_shouldAckAlertAndWritePolicy() {
        UUID exposedApiId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        SecurityAlertEntity alert = alert(exposedApiId.toString());
        alert.setId(alertId);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(exposedApiRepository.existsById(exposedApiId)).thenReturn(true);
        when(accessPolicyRepository.findByExposedApiIdAndMatchTypeAndMatchValue(exposedApiId, AccessPolicyMatchType.IP, "10.0.0.5"))
                .thenReturn(Optional.empty());
        when(accessPolicyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAlertTemporaryBlacklistRequest request = new SecurityAlertTemporaryBlacklistRequest();
        request.setTargetType(SecurityAlertActionTargetType.IP);
        request.setTargetValue("10.0.0.5");
        request.setDurationMinutes(15);
        request.setReason("Repeated brute force");

        SecurityAlertEntity saved = service.temporaryBlacklist(alertId, request);

        assertThat(saved.getStatus()).isEqualTo(SecurityAlertStatus.ACKED);
        ArgumentCaptor<AccessPolicyEntity> policyCaptor = ArgumentCaptor.forClass(AccessPolicyEntity.class);
        verify(accessPolicyRepository).save(policyCaptor.capture());
        assertThat(policyCaptor.getValue().getMatchType()).isEqualTo(AccessPolicyMatchType.IP);
        assertThat(policyCaptor.getValue().getExpiresAt()).isNotNull();
        verify(redisSyncService).syncByExposedApiId(exposedApiId);
    }

    @Test
    void temporaryBlacklist_whenAlertEndpointIdIsExposedApiEndpointId_shouldResolveExposedApiId() {
        UUID exposedApiId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        SecurityAlertEntity alert = alert(endpointId.toString());
        alert.setId(alertId);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(exposedApiRepository.findByEndpointId(endpointId)).thenReturn(Optional.of(ExposedApiEntity.builder()
                .id(exposedApiId)
                .endpointId(endpointId)
                .build()));
        when(accessPolicyRepository.findByExposedApiIdAndMatchTypeAndMatchValue(exposedApiId, AccessPolicyMatchType.CLIENT_ID, "79d0f072-3017-4fc2-aff5-dd7ceda614d4"))
                .thenReturn(Optional.empty());
        when(accessPolicyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAlertTemporaryBlacklistRequest request = new SecurityAlertTemporaryBlacklistRequest();
        request.setTargetType(SecurityAlertActionTargetType.CLIENT);
        request.setTargetValue("79d0f072-3017-4fc2-aff5-dd7ceda614d4");
        request.setDurationMinutes(15);
        request.setReason("Manual review");

        service.temporaryBlacklist(alertId, request);

        ArgumentCaptor<AccessPolicyEntity> policyCaptor = ArgumentCaptor.forClass(AccessPolicyEntity.class);
        verify(accessPolicyRepository).save(policyCaptor.capture());
        assertThat(policyCaptor.getValue().getExposedApiId()).isEqualTo(exposedApiId);
        assertThat(policyCaptor.getValue().getMatchType()).isEqualTo(AccessPolicyMatchType.CLIENT_ID);
        verify(redisSyncService).syncByExposedApiId(exposedApiId);
    }

    private SecurityAlertEntity alert(String endpointId) {
        return SecurityAlertEntity.builder()
                .id(UUID.randomUUID())
                .alertType("AUTH_BRUTE_FORCE")
                .severity(AnomalySeverity.HIGH)
                .status(SecurityAlertStatus.OPEN)
                .fingerprint("fp")
                .endpointId(endpointId)
                .metric("auth_fail_count")
                .scopeType(AnomalyScopeType.ENDPOINT_CLIENT)
                .currentValue(BigDecimal.TEN)
                .thresholdValue(BigDecimal.ONE)
                .windowSeconds(60)
                .count(10L)
                .build();
    }
}
