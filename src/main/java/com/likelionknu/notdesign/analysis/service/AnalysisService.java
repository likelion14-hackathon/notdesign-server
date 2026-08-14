package com.likelionknu.notdesign.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelionknu.notdesign.analysis.client.AnalyzeClient;
import com.likelionknu.notdesign.analysis.client.S3Uploader;
import com.likelionknu.notdesign.analysis.data.dto.response.AnalyzeAcceptedDto;
import com.likelionknu.notdesign.analysis.data.dto.response.AnalyzeResultDto;
import com.likelionknu.notdesign.analysis.data.dto.response.DiaryResultDto;
import com.likelionknu.notdesign.analysis.exception.AnalysisResultNotFoundException;
import com.likelionknu.notdesign.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {
    private static final String STATUS_DONE = "done";
    // Spring Boot 4(Jackson 3)에서는 Jackson 2 ObjectMapper 빈이 자동 등록되지 않아 직접 생성한다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AnalyzeClient analyzeClient;
    private final S3Uploader s3Uploader;
    private final RedisService redisService;

    public String requestAnalyze(MultipartFile image) {
        String imageUrl = s3Uploader.upload(image);
        AnalyzeAcceptedDto accepted = analyzeClient.requestAnalyze(imageUrl);
        log.info("[requestAnalyze] 분석 요청 수락: requestId={}", accepted.requestId());
        return accepted.requestId();
    }

    public AnalyzeResultDto getAnalyzeResult(String requestId) {
        return readResult(requestId + ":analyze", requestId, AnalyzeResultDto.class);
    }

    public String requestDiary(MultipartFile image) {
        String imageUrl = s3Uploader.upload(image);
        AnalyzeAcceptedDto accepted = analyzeClient.requestDiary(imageUrl);
        log.info("[requestDiary] 체험 분석 요청 수락: requestId={}", accepted.requestId());
        return accepted.requestId();
    }

    public DiaryResultDto getDiaryResult(String requestId) {
        return readResult(requestId + ":diary", requestId, DiaryResultDto.class);
    }

    // Redis의 평문 JSON을 읽어 status=done이면 result를 반환, 그 외(없음/processing/failed)는 404.
    private <T> T readResult(String redisKey, String requestId, Class<T> type) {
        Object raw = redisService.getValues(redisKey);
        if (raw == null) {
            throw new AnalysisResultNotFoundException();
        }

        JsonNode root = parse(raw.toString(), requestId);
        String status = root.path("status").asText("");
        if (!STATUS_DONE.equals(status)) {
            log.debug("[readResult] 아직 완료 아님: requestId={}, status={}", requestId, status);
            throw new AnalysisResultNotFoundException();
        }

        JsonNode resultNode = root.get("result");
        if (resultNode == null || resultNode.isNull()) {
            log.error("[readResult] done이지만 result 없음: requestId={}", requestId);
            throw new AnalysisResultNotFoundException();
        }

        return convert(resultNode, requestId, type);
    }

    private JsonNode parse(String json, String requestId) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            log.error("[readResult] Redis 문서 파싱 실패: requestId={}, message={}", requestId, e.getMessage());
            throw new AnalysisResultNotFoundException();
        }
    }

    private <T> T convert(JsonNode resultNode, String requestId, Class<T> type) {
        try {
            return OBJECT_MAPPER.treeToValue(resultNode, type);
        } catch (Exception e) {
            log.error("[readResult] result 변환 실패: requestId={}, message={}", requestId, e.getMessage());
            throw new AnalysisResultNotFoundException();
        }
    }
}
