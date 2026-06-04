package com.pm.be.controller;

import com.pm.be.dto.request.ClientPermissionGrantRequest;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.ClientPermissionResponse;
import com.pm.be.service.ClientPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clients/{clientId}/permissions")
public class ClientPermissionController {
    private final ClientPermissionService clientPermissionService;

    @GetMapping
    public ApiResponse<List<ClientPermissionResponse>> getByClient(@PathVariable UUID clientId) {
        return ApiResponse.<List<ClientPermissionResponse>>builder()
                .result(clientPermissionService.getByClient(clientId))
                .build();
    }

    @PostMapping
    public ApiResponse<ClientPermissionResponse> grant(
            @PathVariable UUID clientId,
            @RequestBody ClientPermissionGrantRequest request) {
        return ApiResponse.<ClientPermissionResponse>builder()
                .result(clientPermissionService.grant(clientId, request))
                .build();
    }

    @PatchMapping("/{permissionId}/enable")
    public ApiResponse<ClientPermissionResponse> enable(
            @PathVariable UUID clientId,
            @PathVariable UUID permissionId) {
        return ApiResponse.<ClientPermissionResponse>builder()
                .result(clientPermissionService.enable(clientId, permissionId))
                .build();
    }

    @PatchMapping("/{permissionId}/disable")
    public ApiResponse<ClientPermissionResponse> disable(
            @PathVariable UUID clientId,
            @PathVariable UUID permissionId) {
        return ApiResponse.<ClientPermissionResponse>builder()
                .result(clientPermissionService.disable(clientId, permissionId))
                .build();
    }

    @DeleteMapping("/{permissionId}")
    public ApiResponse<Void> delete(@PathVariable UUID clientId, @PathVariable UUID permissionId) {
        clientPermissionService.delete(clientId, permissionId);
        return ApiResponse.<Void>builder().build();
    }
}
