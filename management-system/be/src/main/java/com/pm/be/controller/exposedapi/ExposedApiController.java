package com.pm.be.controller.exposedapi;

import com.pm.be.dto.request.exposedapi.ExposedApiLimitUpdateRequest;
import com.pm.be.dto.request.exposedapi.ExposedApiNotificationRuleUpdateRequest;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.exposedapi.ExposedApiResponse;
import com.pm.be.enums.SyncStatus;
import com.pm.be.service.exposedapi.ExposedApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/exposed-apis")
public class ExposedApiController {

    private final ExposedApiService exposedApiService;

    @GetMapping
    public ApiResponse<List<ExposedApiResponse>> getAll(
            @RequestParam(required = false) UUID microServiceId,
            @RequestParam(required = false) SyncStatus syncStatus) {
        return ApiResponse.<List<ExposedApiResponse>>builder()
                .result(exposedApiService.getAll(microServiceId, syncStatus))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ExposedApiResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<ExposedApiResponse>builder()
                .result(exposedApiService.getById(id))
                .build();
    }

    @PatchMapping("/{id}/limits")
    public ApiResponse<ExposedApiResponse> updateLimits(
            @PathVariable UUID id,
            @RequestBody ExposedApiLimitUpdateRequest request) {
        return ApiResponse.<ExposedApiResponse>builder()
                .result(exposedApiService.updateLimits(id, request))
                .build();
    }

    @PatchMapping("/{id}/use-default-config")
    public ApiResponse<ExposedApiResponse> useDefaultConfig(@PathVariable UUID id) {
        return ApiResponse.<ExposedApiResponse>builder()
                .result(exposedApiService.useDefaultConfig(id))
                .build();
    }

    @PatchMapping("/{id}/enable")
    public ApiResponse<ExposedApiResponse> enable(@PathVariable UUID id) {
        return ApiResponse.<ExposedApiResponse>builder()
                .result(exposedApiService.enable(id))
                .build();
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<ExposedApiResponse> disable(@PathVariable UUID id) {
        return ApiResponse.<ExposedApiResponse>builder()
                .result(exposedApiService.disable(id))
                .build();
    }

    @PatchMapping("/{id}/notification-rule")
    public ApiResponse<ExposedApiResponse> updateNotificationRule(
            @PathVariable UUID id,
            @RequestBody ExposedApiNotificationRuleUpdateRequest request) {
        return ApiResponse.<ExposedApiResponse>builder()
                .result(exposedApiService.updateNotificationRule(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        exposedApiService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
