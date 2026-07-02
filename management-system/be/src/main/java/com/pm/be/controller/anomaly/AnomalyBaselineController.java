package com.pm.be.controller.anomaly;

import com.pm.be.dto.response.ApiResponse;
import com.pm.be.service.anomaly.BaselineCalculationJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/anomaly-baselines")
public class AnomalyBaselineController {
    private final BaselineCalculationJob baselineCalculationJob;

    @PostMapping("/recalculate")
    public ApiResponse<Void> recalculate() {
        baselineCalculationJob.run();
        return ApiResponse.<Void>builder().build();
    }
}
