package com.pm.be.controller;

import com.pm.be.dto.request.ApiDefaultConfigUpsertRequest;
import com.pm.be.dto.response.ApiDefaultConfigResponse;
import com.pm.be.dto.response.ApiResponse;
import com.pm.be.service.ApiDefaultConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/api-default-configs")
public class ApiDefaultConfigController {

    private final ApiDefaultConfigService apiDefaultConfigService;

    @PostMapping
    public ApiResponse<ApiDefaultConfigResponse> upsert(@RequestBody ApiDefaultConfigUpsertRequest request) {
        return ApiResponse.<ApiDefaultConfigResponse>builder()
                .result(apiDefaultConfigService.upsert(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<ApiDefaultConfigResponse>> getAll() {
        return ApiResponse.<List<ApiDefaultConfigResponse>>builder()
                .result(apiDefaultConfigService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiDefaultConfigResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<ApiDefaultConfigResponse>builder()
                .result(apiDefaultConfigService.getById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        apiDefaultConfigService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
