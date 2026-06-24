package com.pm.be.controller.client;

import com.pm.be.dto.request.client.ClientCreateRequest;
import com.pm.be.dto.request.client.ClientRevokeRequest;
import com.pm.be.dto.request.client.ClientUpdateRequest;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.client.ClientResponse;
import com.pm.be.enums.ClientStatus;
import com.pm.be.service.client.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clients")
public class ClientController {
    private final ClientService clientService;

    @GetMapping
    public ApiResponse<List<ClientResponse>> getAll(
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) String search) {
        return ApiResponse.<List<ClientResponse>>builder()
                .result(clientService.getAll(status, search))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<ClientResponse>builder()
                .result(clientService.getById(id))
                .build();
    }

    @PostMapping
    public ApiResponse<ClientResponse> create(@RequestBody ClientCreateRequest request) {
        return ApiResponse.<ClientResponse>builder()
                .result(clientService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ClientResponse> update(@PathVariable UUID id, @RequestBody ClientUpdateRequest request) {
        return ApiResponse.<ClientResponse>builder()
                .result(clientService.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<ClientResponse> activate(@PathVariable UUID id) {
        return ApiResponse.<ClientResponse>builder()
                .result(clientService.activate(id))
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<ClientResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.<ClientResponse>builder()
                .result(clientService.deactivate(id))
                .build();
    }

    @PatchMapping("/{id}/revoke")
    public ApiResponse<ClientResponse> revoke(@PathVariable UUID id, @RequestBody ClientRevokeRequest request) {
        return ApiResponse.<ClientResponse>builder()
                .result(clientService.revoke(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
