package com.likelionknu.notdesign.result.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.result.data.dto.request.ResultCreateRequestDto;
import com.likelionknu.notdesign.result.data.dto.response.ResultResponseDto;
import com.likelionknu.notdesign.result.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultController {
    private final ResultService resultService;

    @PostMapping
    @Operation(summary = "측정 결과 생성 (더미 복사)")
    public GlobalResponse<ResultResponseDto> createResult(
            @RequestBody(required = false) ResultCreateRequestDto request) {
        return GlobalResponse.ok(resultService.createResult(SecurityUtil.getUsername(), request));
    }

    @GetMapping
    @Operation(summary = "내 측정 결과 목록")
    public GlobalResponse<List<ResultResponseDto>> getResults() {
        return GlobalResponse.ok(resultService.getResults(SecurityUtil.getUsername()));
    }

    @GetMapping("/{resultId}")
    @Operation(summary = "측정 결과 상세")
    public GlobalResponse<ResultResponseDto> getResult(@PathVariable Long resultId) {
        return GlobalResponse.ok(resultService.getResult(SecurityUtil.getUsername(), resultId));
    }
}
