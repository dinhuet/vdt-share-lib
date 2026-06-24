package com.pm.be.controller.microservice;

import com.pm.be.dto.response.ApiResponse;
import com.pm.be.dto.response.microservice.MicroServiceResponse;
import com.pm.be.service.microservice.MicroServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/micro-services")
public class MicroServiceController {
    private final MicroServiceService microServiceService;

    @GetMapping
    public ApiResponse<List<MicroServiceResponse>> getAll() {
        return ApiResponse.<List<MicroServiceResponse>>builder()
                .result(microServiceService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MicroServiceResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<MicroServiceResponse>builder()
                .result(microServiceService.getById(id))
                .build();
    }
}
