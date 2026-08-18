package com.likelionknu.notdesign.analysis.controller;

import com.likelionknu.notdesign.analysis.data.dto.response.AnalyzeResultDto;
import com.likelionknu.notdesign.analysis.data.dto.response.DiaryResultDto;
import com.likelionknu.notdesign.analysis.data.dto.response.RequestIdResponseDto;
import com.likelionknu.notdesign.analysis.service.AnalysisService;
import com.likelionknu.notdesign.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "피부 이미지 분석 요청",
            description = "얼굴 이미지 파일(image)을 업로드하면 S3 저장 후 analyze 서버 호출, 폴링용 requestId 반환")
    public GlobalResponse<RequestIdResponseDto> requestAnalyze(@RequestPart("image") MultipartFile image) {
        String requestId = analysisService.requestAnalyze(image);
        return GlobalResponse.ok(RequestIdResponseDto.of(requestId));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "피부 이미지 분석 결과 조회", description = "done이면 결과 반환, 그 외(진행중/실패/없음)는 404")
    public GlobalResponse<AnalyzeResultDto> getAnalyzeResult(@PathVariable String requestId) {
        return GlobalResponse.ok(analysisService.getAnalyzeResult(requestId));
    }

    @PostMapping(value = "/diary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "체험 피부 이미지 분석 요청",
            description = "얼굴 이미지 파일(image)을 업로드하면 S3 저장 후 analyze 서버(/analyze/diary) 호출, 폴링용 requestId 반환")
    public GlobalResponse<RequestIdResponseDto> requestDiary(@RequestPart("image") MultipartFile image) {
        String requestId = analysisService.requestDiary(image);
        return GlobalResponse.ok(RequestIdResponseDto.of(requestId));
    }

    @GetMapping("/diary/{requestId}")
    @Operation(summary = "체험 피부 이미지 분석 결과 조회", description = "done이면 결과 반환, 그 외(진행중/실패/없음)는 404")
    public GlobalResponse<DiaryResultDto> getDiaryResult(@PathVariable String requestId) {
        return GlobalResponse.ok(analysisService.getDiaryResult(requestId));
    }
}
