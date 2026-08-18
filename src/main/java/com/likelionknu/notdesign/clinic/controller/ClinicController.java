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

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
public class ClinicController {
    private final ClinicService clinicService;

    @GetMapping
    @Operation(summary = "클리닉 목록 조회")
    public GlobalResponse<List<ClinicResponseDto>> getClinics() {
        return GlobalResponse.ok(clinicService.getClinics());
    }
}
