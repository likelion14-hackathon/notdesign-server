package com.likelionknu.notdesign.diagnosis.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.diagnosis.data.dto.request.DiagnosisCreateRequestDto;
import com.likelionknu.notdesign.diagnosis.data.dto.response.DiagnosisResponseDto;
import com.likelionknu.notdesign.diagnosis.service.DiagnosisService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {
    private final DiagnosisService diagnosisService;

    @PostMapping
    @Operation(summary = "웰니스 지출 진단 생성")
    public GlobalResponse<DiagnosisResponseDto> createDiagnosis(@Valid @RequestBody DiagnosisCreateRequestDto request) {
        return GlobalResponse.ok(diagnosisService.createDiagnosis(SecurityUtil.getUsername(), request));
    }
}
