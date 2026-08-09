package com.likelionknu.notdesign.clinic.controller;

import com.likelionknu.notdesign.clinic.data.dto.response.ClinicResponseDto;
import com.likelionknu.notdesign.clinic.service.ClinicService;
import com.likelionknu.notdesign.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클리닉 관련 API 엔드포인트.
 */
@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
public class ClinicController {
    private final ClinicService clinicService;

    /**
     * 클리닉 목록 조회.
     *
     * @return 전체 클리닉 목록
     */
    @GetMapping
    @Operation(summary = "클리닉 목록 조회")
    public GlobalResponse<List<ClinicResponseDto>> getClinics() {
        return clinicService.getClinics();
    }
}
