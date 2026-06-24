package com.pm.be.controller.clientcredential;

import com.pm.be.dto.request.clientcredential.ClientCredentialCreateRequest;
import com.pm.be.dto.request.clientcredential.ClientCredentialRevokeRequest;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.clientcredential.ClientCredentialCreatedResponse;
import com.pm.be.dto.response.clientcredential.ClientCredentialResponse;
import com.pm.be.service.clientcredential.ClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clients/{clientId}/credentials")
public class ClientCredentialController {
    private final ClientCredentialService clientCredentialService;

    @GetMapping
    public ApiResponse<List<ClientCredentialResponse>> getAll(
            @PathVariable UUID clientId,
            @RequestParam(required = false) UUID microServiceId) {
        return ApiResponse.<List<ClientCredentialResponse>>builder()
                .result(clientCredentialService.getAll(clientId, microServiceId))
                .build();
    }

    @PostMapping
    public ApiResponse<ClientCredentialCreatedResponse> create(
            @PathVariable UUID clientId,
            @RequestBody ClientCredentialCreateRequest request) {
        return ApiResponse.<ClientCredentialCreatedResponse>builder()
                .result(clientCredentialService.create(clientId, request))
                .build();
    }

    @PatchMapping("/{credentialId}/revoke")
    public ApiResponse<ClientCredentialResponse> revoke(
            @PathVariable UUID clientId,
            @PathVariable UUID credentialId,
            @RequestBody ClientCredentialRevokeRequest request) {
        return ApiResponse.<ClientCredentialResponse>builder()
                .result(clientCredentialService.revoke(clientId, credentialId, request))
                .build();
    }
}
