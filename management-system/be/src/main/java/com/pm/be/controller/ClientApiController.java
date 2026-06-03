package com.pm.be.controller;

import com.pm.be.dto.request.ClientApiUpdateRequest;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.ClientApiResponse;
import com.pm.be.enums.SyncStatus;
import com.pm.be.service.ClientApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/client-apis")
public class ClientApiController {

    private final ClientApiService clientApiService;

    @GetMapping
    public ApiResponse<List<ClientApiResponse>> getAll(
            @RequestParam(required = false) UUID microServiceId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) SyncStatus syncStatus) {
        return ApiResponse.<List<ClientApiResponse>>builder()
                .result(clientApiService.getAll(microServiceId, clientId, enabled, syncStatus))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientApiResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<ClientApiResponse>builder()
                .result(clientApiService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ClientApiResponse> update(@PathVariable UUID id, @RequestBody ClientApiUpdateRequest request) {
        return ApiResponse.<ClientApiResponse>builder()
                .result(clientApiService.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/enable")
    public ApiResponse<ClientApiResponse> enable(@PathVariable UUID id) {
        return ApiResponse.<ClientApiResponse>builder()
                .result(clientApiService.enable(id))
                .build();
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<ClientApiResponse> disable(@PathVariable UUID id) {
        return ApiResponse.<ClientApiResponse>builder()
                .result(clientApiService.disable(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        clientApiService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
