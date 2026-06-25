package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyOccurrenceProperties;
import com.pm.be.dto.anomaly.AnomalyRuleMatch;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.anomaly.SecurityAlertOccurrenceEntity;
import com.pm.be.repository.anomaly.SecurityAlertOccurrenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAlertOccurrenceService {
    private final SecurityAlertOccurrenceRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final AnomalyOccurrenceProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<SecurityAlertOccurrenceEntity> createIfAllowed(SecurityAlertEntity alert, AnomalyRuleMatch match) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || alert == null || alert.getId() == null || match == null) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(properties.getCooldownEnabled()) && !reserveCooldown(alert.getFingerprint(), match)) {
            return Optional.empty();
        }

        SecurityAlertOccurrenceEntity occurrence = SecurityAlertOccurrenceEntity.builder()
                .alertId(alert.getId())
                .ruleCode(match.ruleCode())
                .ruleType(match.ruleType())
                .metric(match.metric())
                .scopeType(match.scopeType())
                .scopeKey(match.scopeKey())
                .currentValue(match.currentValue())
                .thresholdValue(match.thresholdValue())
                .baselineValue(match.baselineValue())
                .windowSeconds(match.windowSeconds())
                .windowStart(toLocalDateTime(match.windowStart()))
                .windowEnd(toLocalDateTime(match.windowEnd()))
                .timeBucket(match.timeBucket())
                .eventTimestamp(toLocalDateTime(match.event() == null ? null : match.event().getTimestamp()))
                .build();
        return Optional.of(repository.save(occurrence));
    }

    public List<SecurityAlertOccurrenceEntity> findByAlertId(UUID alertId) {
        return repository.findByAlertIdOrderByCreatedAtDesc(alertId);
    }

    private boolean reserveCooldown(String fingerprint, AnomalyRuleMatch match) {
        String windowStart = match.windowStart() == null ? "unknown" : match.windowStart().toString();
        String key = "vdt:anomaly:occurrence-cooldown:" + fingerprint + ":" + windowStart;
        int windowSeconds = match.windowSeconds() == null || match.windowSeconds() <= 0 ? 60 : match.windowSeconds();
        try {
            Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(windowSeconds * 2L));
            return Boolean.TRUE.equals(reserved);
        } catch (RuntimeException e) {
            log.warn("Failed to reserve occurrence cooldown; occurrence will be created: key={}", key, e);
            return true;
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
