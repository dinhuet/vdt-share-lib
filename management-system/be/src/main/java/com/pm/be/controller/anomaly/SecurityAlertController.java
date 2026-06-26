package com.pm.be.controller.anomaly;

import com.pm.be.dto.request.anomaly.SecurityAlertActionRequest;
import com.pm.be.dto.request.anomaly.SecurityAlertTemporaryBlacklistRequest;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.anomaly.NotificationDeliveryResponse;
import com.pm.be.dto.response.anomaly.SecurityAlertOccurrenceResponse;
import com.pm.be.dto.response.anomaly.SecurityAlertResponse;
import com.pm.be.dto.response.anomaly.SecurityAlertSummaryResponse;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.service.anomaly.SecurityAlertActionService;
import com.pm.be.service.anomaly.SecurityAlertBlacklistService;
import com.pm.be.service.anomaly.SecurityAlertQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/security-alerts")
public class SecurityAlertController {
    private final SecurityAlertQueryService queryService;
    private final SecurityAlertActionService actionService;
    private final SecurityAlertBlacklistService blacklistService;

    @GetMapping
    public ApiResponse<List<SecurityAlertResponse>> search(
            @RequestParam(required = false) SecurityAlertStatus status,
            @RequestParam(required = false) AnomalySeverity severity,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String endpointId,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String sourceIp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.<List<SecurityAlertResponse>>builder()
                .result(queryService.search(status, severity, ruleCode, ruleType, serviceName, endpointId, clientId, sourceIp, from, to))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SecurityAlertResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<SecurityAlertResponse>builder()
                .result(queryService.getById(id))
                .build();
    }

    @GetMapping("/summary")
    public ApiResponse<SecurityAlertSummaryResponse> summary() {
        return ApiResponse.<SecurityAlertSummaryResponse>builder()
                .result(queryService.summary())
                .build();
    }

    @GetMapping("/recent")
    public ApiResponse<List<SecurityAlertResponse>> recent(@RequestParam(required = false) Integer limit) {
        return ApiResponse.<List<SecurityAlertResponse>>builder()
                .result(queryService.recent(limit))
                .build();
    }

    @GetMapping("/{id}/occurrences")
    public ApiResponse<List<SecurityAlertOccurrenceResponse>> occurrences(@PathVariable UUID id) {
        return ApiResponse.<List<SecurityAlertOccurrenceResponse>>builder()
                .result(queryService.occurrences(id))
                .build();
    }

    @GetMapping("/{id}/notifications")
    public ApiResponse<List<NotificationDeliveryResponse>> notifications(@PathVariable UUID id) {
        return ApiResponse.<List<NotificationDeliveryResponse>>builder()
                .result(queryService.notifications(id))
                .build();
    }

    @PatchMapping("/{id}/ack")
    public ApiResponse<SecurityAlertResponse> ack(@PathVariable UUID id, @RequestBody(required = false) SecurityAlertActionRequest request) {
        return ApiResponse.<SecurityAlertResponse>builder()
                .result(queryService.toResponse(actionService.ack(id, request)))
                .build();
    }

    @PatchMapping("/{id}/ignore")
    public ApiResponse<SecurityAlertResponse> ignore(@PathVariable UUID id, @RequestBody(required = false) SecurityAlertActionRequest request) {
        return ApiResponse.<SecurityAlertResponse>builder()
                .result(queryService.toResponse(actionService.ignore(id, request)))
                .build();
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<SecurityAlertResponse> resolve(@PathVariable UUID id, @RequestBody(required = false) SecurityAlertActionRequest request) {
        return ApiResponse.<SecurityAlertResponse>builder()
                .result(queryService.toResponse(actionService.resolve(id, request)))
                .build();
    }

    @PostMapping("/{id}/blacklist-temporary")
    public ApiResponse<SecurityAlertResponse> blacklistTemporary(@PathVariable UUID id,
                                                                 @RequestBody SecurityAlertTemporaryBlacklistRequest request) {
        return ApiResponse.<SecurityAlertResponse>builder()
                .result(queryService.toResponse(blacklistService.temporaryBlacklist(id, request)))
                .build();
    }
}
