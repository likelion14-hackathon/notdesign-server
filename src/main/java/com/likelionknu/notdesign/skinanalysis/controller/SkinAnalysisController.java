package com.likelionknu.notdesign.skinanalysis.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.skinanalysis.data.dto.request.SkinAnalysisRequestDto;
import com.likelionknu.notdesign.skinanalysis.data.dto.response.AnalyzeResultDto;
import com.likelionknu.notdesign.skinanalysis.data.dto.response.RequestIdResponseDto;
import com.likelionknu.notdesign.skinanalysis.service.SkinAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피부 이미지 분석(analyze) 연동 컨트롤러.
 * 요청은 analyze 서버를 호출해 request_id 를 반환하고,
 * 조회는 클라우드 Redis 에서 결과를 읽어 done 이면 반환, 아니면 404.
 */
@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class SkinAnalysisController {
    private final SkinAnalysisService skinAnalysisService;

    @PostMapping
    @Operation(summary = "피부 이미지 분석 요청", description = "imageUrl 로 analyze 서버 호출 후 폴링용 requestId 반환")
    public GlobalResponse<RequestIdResponseDto> requestAnalyze(
            @Valid @RequestBody SkinAnalysisRequestDto request) {
        String requestId = skinAnalysisService.requestAnalyze(request.imageUrl());
        return GlobalResponse.ok(RequestIdResponseDto.of(requestId));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "피부 이미지 분석 결과 조회", description = "done 이면 결과 반환, 그 외(진행중/실패/없음)는 404")
    public GlobalResponse<AnalyzeResultDto> getAnalyzeResult(@PathVariable String requestId) {
        return GlobalResponse.ok(skinAnalysisService.getAnalyzeResult(requestId));
    }
}
