package com.pm.be.controller.anomaly;

import com.pm.be.dto.request.anomaly.AnomalyRuleEnabledUpdateRequest;
import com.pm.be.dto.request.anomaly.AnomalyRuleUpsertRequest;
import com.pm.be.dto.response.anomaly.AnomalyRuleResponse;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.service.anomaly.AnomalyRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/anomaly-rules")
public class AnomalyRuleController {
    private final AnomalyRuleService anomalyRuleService;

    @GetMapping
    public ApiResponse<List<AnomalyRuleResponse>> getAll() {
        return ApiResponse.<List<AnomalyRuleResponse>>builder()
                .result(anomalyRuleService.getAll())
                .build();
    }

    @GetMapping("/{ruleId}")
    public ApiResponse<AnomalyRuleResponse> getById(@PathVariable UUID ruleId) {
        return ApiResponse.<AnomalyRuleResponse>builder()
                .result(anomalyRuleService.getById(ruleId))
                .build();
    }

    @PostMapping
    public ApiResponse<AnomalyRuleResponse> create(@RequestBody AnomalyRuleUpsertRequest request) {
        return ApiResponse.<AnomalyRuleResponse>builder()
                .result(anomalyRuleService.create(request))
                .build();
    }

    @PutMapping("/{ruleId}")
    public ApiResponse<AnomalyRuleResponse> update(
            @PathVariable UUID ruleId,
            @RequestBody AnomalyRuleUpsertRequest request) {
        return ApiResponse.<AnomalyRuleResponse>builder()
                .result(anomalyRuleService.update(ruleId, request))
                .build();
    }

    @PatchMapping("/{ruleId}/enabled")
    public ApiResponse<AnomalyRuleResponse> updateEnabled(
            @PathVariable UUID ruleId,
            @RequestBody AnomalyRuleEnabledUpdateRequest request) {
        return ApiResponse.<AnomalyRuleResponse>builder()
                .result(anomalyRuleService.updateEnabled(ruleId, request))
                .build();
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> delete(@PathVariable UUID ruleId) {
        anomalyRuleService.delete(ruleId);
        return ApiResponse.<Void>builder().build();
    }
}
