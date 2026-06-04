package com.pm.be.controller;

import com.pm.be.dto.request.AccessPolicyUpsertRequest;
import com.pm.be.dto.response.AccessPolicyResponse;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.service.AccessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/exposed-apis/{exposedApiId}/access-policies")
public class AccessPolicyController {
    private final AccessPolicyService accessPolicyService;

    @GetMapping
    public ApiResponse<List<AccessPolicyResponse>> getAll(@PathVariable UUID exposedApiId) {
        return ApiResponse.<List<AccessPolicyResponse>>builder()
                .result(accessPolicyService.getAll(exposedApiId))
                .build();
    }

    @PostMapping
    public ApiResponse<AccessPolicyResponse> create(
            @PathVariable UUID exposedApiId,
            @RequestBody AccessPolicyUpsertRequest request) {
        return ApiResponse.<AccessPolicyResponse>builder()
                .result(accessPolicyService.create(exposedApiId, request))
                .build();
    }

    @PutMapping("/{policyId}")
    public ApiResponse<AccessPolicyResponse> update(
            @PathVariable UUID exposedApiId,
            @PathVariable UUID policyId,
            @RequestBody AccessPolicyUpsertRequest request) {
        return ApiResponse.<AccessPolicyResponse>builder()
                .result(accessPolicyService.update(exposedApiId, policyId, request))
                .build();
    }

    @DeleteMapping("/{policyId}")
    public ApiResponse<Void> delete(@PathVariable UUID exposedApiId, @PathVariable UUID policyId) {
        accessPolicyService.delete(exposedApiId, policyId);
        return ApiResponse.<Void>builder().build();
    }
}
