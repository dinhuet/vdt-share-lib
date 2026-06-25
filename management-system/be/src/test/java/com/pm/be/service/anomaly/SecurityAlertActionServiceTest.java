package com.pm.be.service.anomaly;

import com.pm.be.dto.request.anomaly.SecurityAlertActionRequest;
import com.pm.be.entity.anomaly.SecurityAlertActionEntity;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertActionType;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.repository.anomaly.SecurityAlertActionRepository;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
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
class SecurityAlertActionServiceTest {
    @Mock SecurityAlertRepository alertRepository;
    @Mock SecurityAlertActionRepository actionRepository;

    @Test
    void ack_shouldUpdateStatusAndWriteAction() {
        UUID alertId = UUID.randomUUID();
        SecurityAlertEntity alert = alert(alertId);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityAlertActionService service = new SecurityAlertActionService(alertRepository, actionRepository);

        SecurityAlertActionRequest request = new SecurityAlertActionRequest();
        request.setReason("Investigating");
        SecurityAlertEntity saved = service.ack(alertId, request);

        assertThat(saved.getStatus()).isEqualTo(SecurityAlertStatus.ACKED);
        ArgumentCaptor<SecurityAlertActionEntity> captor = ArgumentCaptor.forClass(SecurityAlertActionEntity.class);
        verify(actionRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(SecurityAlertActionType.ACK);
        assertThat(captor.getValue().getReason()).isEqualTo("Investigating");
    }

    private SecurityAlertEntity alert(UUID id) {
        return SecurityAlertEntity.builder()
                .id(id)
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
}
