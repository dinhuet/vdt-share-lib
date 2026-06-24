package com.pm.be.service.anomaly;

import com.pm.be.dto.request.anomaly.AnomalyBaselineRuleConfigRequest;
import com.pm.be.dto.request.anomaly.AnomalyRuleEnabledUpdateRequest;
import com.pm.be.dto.request.anomaly.AnomalyRuleUpsertRequest;
import com.pm.be.dto.request.anomaly.AnomalyStaticRuleConfigRequest;
import com.pm.be.dto.response.anomaly.AnomalyBaselineRuleConfigResponse;
import com.pm.be.dto.response.anomaly.AnomalyRuleResponse;
import com.pm.be.dto.response.anomaly.AnomalyStaticRuleConfigResponse;
import com.pm.be.entity.anomaly.AnomalyBaselineRuleConfigEntity;
import com.pm.be.entity.anomaly.AnomalyRuleEntity;
import com.pm.be.entity.anomaly.AnomalyStaticRuleConfigEntity;
import com.pm.be.enums.AnomalyRuleOperator;
import com.pm.be.enums.AnomalyRuleType;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.anomaly.AnomalyRuleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnomalyRuleService {
    private final AnomalyRuleRepository anomalyRuleRepo;

    public List<AnomalyRuleResponse> getAll() {
        return anomalyRuleRepo.findAll().stream()
                .sorted(Comparator.comparing(AnomalyRuleEntity::getRuleCode))
                .map(this::toResponse)
                .toList();
    }

    public AnomalyRuleResponse getById(UUID ruleId) {
        return toResponse(getRule(ruleId));
    }

    public AnomalyRuleResponse create(AnomalyRuleUpsertRequest request) {
        validateRuleRequest(request);
        String ruleCode = normalizeCode(request.getRuleCode());
        if (anomalyRuleRepo.existsByRuleCode(ruleCode)) {
            throw new AppException(ErrorCode.ANOMALY_RULE_EXISTED);
        }

        AnomalyRuleEntity entity = buildRuleEntity(request, ruleCode);
        applyConfigs(entity, request);
        return toResponse(anomalyRuleRepo.save(entity));
    }

    public AnomalyRuleResponse update(UUID ruleId, AnomalyRuleUpsertRequest request) {
        validateRuleRequest(request);
        AnomalyRuleEntity entity = getRule(ruleId);
        String ruleCode = normalizeCode(request.getRuleCode());
        anomalyRuleRepo.findByRuleCode(ruleCode)
                .filter(existing -> !existing.getId().equals(ruleId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.ANOMALY_RULE_EXISTED);
                });

        entity.setRuleCode(ruleCode);
        entity.setName(request.getName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setRuleType(request.getRuleType());
        entity.setMetric(request.getMetric().trim());
        entity.setSeverity(request.getSeverity());
        entity.setScopeType(request.getScopeType());
        entity.setScopeId(trimToNull(request.getScopeId()));
        entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        entity.setNotificationRuleId(request.getNotificationRuleId());
        entity.setCooldownMinutes(request.getCooldownMinutes());
        clearConfigs(entity);
        applyConfigs(entity, request);
        return toResponse(anomalyRuleRepo.save(entity));
    }

    public AnomalyRuleResponse updateEnabled(UUID ruleId, AnomalyRuleEnabledUpdateRequest request) {
        if (request == null || request.getEnabled() == null) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
        AnomalyRuleEntity entity = getRule(ruleId);
        entity.setEnabled(request.getEnabled());
        return toResponse(anomalyRuleRepo.save(entity));
    }

    public void delete(UUID ruleId) {
        anomalyRuleRepo.delete(getRule(ruleId));
    }

    private AnomalyRuleEntity getRule(UUID ruleId) {
        if (ruleId == null) {
            throw new AppException(ErrorCode.ANOMALY_RULE_NOTFOUND);
        }
        return anomalyRuleRepo.findById(ruleId)
                .orElseThrow(() -> new AppException(ErrorCode.ANOMALY_RULE_NOTFOUND));
    }

    private void validateRuleRequest(AnomalyRuleUpsertRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getRuleCode())
                || !StringUtils.hasText(request.getName())
                || request.getRuleType() == null
                || !StringUtils.hasText(request.getMetric())
                || request.getSeverity() == null
                || request.getScopeType() == null) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
        if (request.getScopeType() != AnomalyScopeType.GLOBAL && !StringUtils.hasText(request.getScopeId())) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
        if (request.getScopeType() == AnomalyScopeType.GLOBAL && StringUtils.hasText(request.getScopeId())) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
        if (request.getCooldownMinutes() != null && request.getCooldownMinutes() < 0) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
        switch (request.getRuleType()) {
            case STATIC -> validateStaticConfig(request.getStaticConfig());
            case BASELINE -> validateBaselineConfig(request.getBaselineConfig());
            case HYBRID -> {
                validateStaticConfig(request.getStaticConfig());
                validateBaselineConfig(request.getBaselineConfig());
            }
        }
    }

    private void validateStaticConfig(AnomalyStaticRuleConfigRequest request) {
        if (request == null
                || !isPositive(request.getThresholdValue())
                || request.getWindowSeconds() == null || request.getWindowSeconds() <= 0
                || request.getMinSampleCount() == null || request.getMinSampleCount() <= 0
                || request.getConsecutiveWindows() == null || request.getConsecutiveWindows() <= 0
                || request.getOperator() == null) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
    }

    private void validateBaselineConfig(AnomalyBaselineRuleConfigRequest request) {
        if (request == null
                || request.getHistoryDays() == null || request.getHistoryDays() <= 0
                || request.getTimeBucketType() == null
                || !isPositive(request.getPercentile()) || request.getPercentile().compareTo(BigDecimal.valueOf(100)) > 0
                || !isPositive(request.getMultiplier())
                || request.getMinAbsoluteThreshold() == null || request.getMinAbsoluteThreshold().compareTo(BigDecimal.ZERO) < 0
                || request.getMinSampleCount() == null || request.getMinSampleCount() <= 0
                || request.getConsecutiveWindows() == null || request.getConsecutiveWindows() <= 0) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
        if (request.getMaxAbsoluteThreshold() != null
                && request.getMaxAbsoluteThreshold().compareTo(request.getMinAbsoluteThreshold()) < 0) {
            throw new AppException(ErrorCode.ANOMALY_RULE_INVALID);
        }
    }

    private AnomalyRuleEntity buildRuleEntity(AnomalyRuleUpsertRequest request, String ruleCode) {
        return AnomalyRuleEntity.builder()
                .ruleCode(ruleCode)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .ruleType(request.getRuleType())
                .metric(request.getMetric().trim())
                .severity(request.getSeverity())
                .scopeType(request.getScopeType())
                .scopeId(trimToNull(request.getScopeId()))
                .enabled(request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()))
                .notificationRuleId(request.getNotificationRuleId())
                .cooldownMinutes(request.getCooldownMinutes())
                .build();
    }

    private void applyConfigs(AnomalyRuleEntity entity, AnomalyRuleUpsertRequest request) {
        if (request.getRuleType() == AnomalyRuleType.STATIC || request.getRuleType() == AnomalyRuleType.HYBRID) {
            AnomalyStaticRuleConfigRequest config = request.getStaticConfig();
            entity.setStaticConfig(AnomalyStaticRuleConfigEntity.builder()
                    .rule(entity)
                    .thresholdValue(config.getThresholdValue())
                    .windowSeconds(config.getWindowSeconds())
                    .minSampleCount(config.getMinSampleCount())
                    .consecutiveWindows(config.getConsecutiveWindows())
                    .operator(config.getOperator())
                    .build());
        }
        if (request.getRuleType() == AnomalyRuleType.BASELINE || request.getRuleType() == AnomalyRuleType.HYBRID) {
            AnomalyBaselineRuleConfigRequest config = request.getBaselineConfig();
            entity.setBaselineConfig(AnomalyBaselineRuleConfigEntity.builder()
                    .rule(entity)
                    .historyDays(config.getHistoryDays())
                    .timeBucketType(config.getTimeBucketType())
                    .percentile(config.getPercentile())
                    .multiplier(config.getMultiplier())
                    .minAbsoluteThreshold(config.getMinAbsoluteThreshold())
                    .maxAbsoluteThreshold(config.getMaxAbsoluteThreshold())
                    .minSampleCount(config.getMinSampleCount())
                    .consecutiveWindows(config.getConsecutiveWindows())
                    .build());
        }
    }

    private void clearConfigs(AnomalyRuleEntity entity) {
        if (entity.getStaticConfig() != null) {
            entity.getStaticConfig().setRule(null);
            entity.setStaticConfig(null);
        }
        if (entity.getBaselineConfig() != null) {
            entity.getBaselineConfig().setRule(null);
            entity.setBaselineConfig(null);
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String normalizeCode(String ruleCode) {
        return ruleCode.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private AnomalyRuleResponse toResponse(AnomalyRuleEntity entity) {
        return AnomalyRuleResponse.builder()
                .id(entity.getId())
                .ruleCode(entity.getRuleCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .ruleType(entity.getRuleType())
                .metric(entity.getMetric())
                .severity(entity.getSeverity())
                .scopeType(entity.getScopeType())
                .scopeId(entity.getScopeId())
                .enabled(entity.getEnabled())
                .notificationRuleId(entity.getNotificationRuleId())
                .cooldownMinutes(entity.getCooldownMinutes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .staticConfig(toStaticResponse(entity.getStaticConfig()))
                .baselineConfig(toBaselineResponse(entity.getBaselineConfig()))
                .build();
    }

    private AnomalyStaticRuleConfigResponse toStaticResponse(AnomalyStaticRuleConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return AnomalyStaticRuleConfigResponse.builder()
                .thresholdValue(entity.getThresholdValue())
                .windowSeconds(entity.getWindowSeconds())
                .minSampleCount(entity.getMinSampleCount())
                .consecutiveWindows(entity.getConsecutiveWindows())
                .operator(entity.getOperator())
                .build();
    }

    private AnomalyBaselineRuleConfigResponse toBaselineResponse(AnomalyBaselineRuleConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return AnomalyBaselineRuleConfigResponse.builder()
                .historyDays(entity.getHistoryDays())
                .timeBucketType(entity.getTimeBucketType())
                .percentile(entity.getPercentile())
                .multiplier(entity.getMultiplier())
                .minAbsoluteThreshold(entity.getMinAbsoluteThreshold())
                .maxAbsoluteThreshold(entity.getMaxAbsoluteThreshold())
                .minSampleCount(entity.getMinSampleCount())
                .consecutiveWindows(entity.getConsecutiveWindows())
                .build();
    }
}
