package com.likelionknu.notdesign.report.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.report.data.dto.response.ReportResponseDto;
import com.likelionknu.notdesign.report.service.ReportGenerateService;
import com.likelionknu.notdesign.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final ReportGenerateService reportGenerateService;

    @PostMapping
    @Operation(summary = "오프라인 측정 데이터 기반 리포트 생성")
    public GlobalResponse<ReportResponseDto> generateReport() {
        return GlobalResponse.ok(reportGenerateService.generateReport(SecurityUtil.getUsername()));
    }

    @GetMapping("/latest")
    @Operation(summary = "최근 리포트 조회")
    public GlobalResponse<ReportResponseDto> getLatestReport() {
        return GlobalResponse.ok(reportService.getLatestReport(SecurityUtil.getUsername()));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "리포트 조회")
    public GlobalResponse<ReportResponseDto> getReport(@PathVariable Long reportId) {
        return GlobalResponse.ok(reportService.getReport(SecurityUtil.getUsername(), reportId));
    }
}
