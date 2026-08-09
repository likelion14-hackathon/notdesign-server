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

/**
 * 측정 결과 관련 API 엔드포인트.
 * 측정 결과 생성(더미 복사)과 본인 결과 조회를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultController {
    private final ResultService resultService;

    /**
     * 측정 결과 생성.
     * 요청한 클리닉의 더미 측정값을 실제 결과로 복사한다.
     *
     * @param request 측정 클리닉 정보
     * @return 생성된 측정 결과
     */
    @PostMapping
    @Operation(summary = "측정 결과 생성 (더미 복사)")
    public GlobalResponse<ResultResponseDto> createResult(
            @RequestBody(required = false) ResultCreateRequestDto request) {
        return resultService.createResult(SecurityUtil.getUsername(), request);
    }

    /**
     * 내 측정 결과 목록 조회.
     *
     * @return 측정 결과 목록(최신순)
     */
    @GetMapping
    @Operation(summary = "내 측정 결과 목록")
    public GlobalResponse<List<ResultResponseDto>> getResults() {
        return resultService.getResults(SecurityUtil.getUsername());
    }

    /**
     * 측정 결과 상세 조회.
     *
     * @param resultId 조회할 측정 결과 ID
     * @return 측정 결과 상세
     */
    @GetMapping("/{resultId}")
    @Operation(summary = "측정 결과 상세")
    public GlobalResponse<ResultResponseDto> getResult(@PathVariable Long resultId) {
        return resultService.getResult(SecurityUtil.getUsername(), resultId);
    }
}
