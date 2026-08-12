package com.likelionknu.notdesign.skinanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelionknu.notdesign.common.redis.RedisService;
import com.likelionknu.notdesign.skinanalysis.client.AnalyzeClient;
import com.likelionknu.notdesign.skinanalysis.client.dto.AnalyzeAcceptedDto;
import com.likelionknu.notdesign.skinanalysis.data.dto.response.AnalyzeResultDto;
import com.likelionknu.notdesign.skinanalysis.exception.AnalysisResultNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 피부 이미지 분석 연동 서비스.
 * - 요청: analyze 서버 호출 후 request_id 만 프론트에 반환(DB 저장 없음).
 * - 조회: 클라우드 Redis 의 {request_id}:analyze 를 직접 읽어 status=done 이면 result 반환, 그 외 404.
 * analyze 서버가 결과를 평문 JSON 문자열로 저장하므로 String 으로 읽어 Jackson 으로 파싱한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisService {
    private static final String STATUS_DONE = "done";
    // Spring Boot 4 는 Jackson 3(tools.jackson)로 전환되어 Jackson 2 ObjectMapper 빈이 자동 등록되지 않는다.
    // Redis 평문 JSON 파싱 전용이라 별도 설정이 필요 없으므로 직접 인스턴스를 생성해 사용한다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AnalyzeClient analyzeClient;
    private final RedisService redisService;

    /**
     * 피부 이미지 분석을 요청하고 폴링용 request_id 를 반환한다.
     *
     * @param imageUrl 공개 접근 가능한 얼굴 이미지 URL
     * @return request_id
     */
    public String requestAnalyze(String imageUrl) {
        AnalyzeAcceptedDto accepted = analyzeClient.requestAnalyze(imageUrl);
        log.info("[requestAnalyze] 분석 요청 수락: requestId={}", accepted.requestId());
        return accepted.requestId();
    }

    /**
     * {request_id}:analyze 결과를 조회한다. done 이 아니면(키 없음/processing/failed) 404.
     *
     * @param requestId 분석 요청 식별자
     * @return analyze 결과(done 인 경우)
     */
    public AnalyzeResultDto getAnalyzeResult(String requestId) {
        return readResult(requestId + ":analyze", requestId);
    }

    private AnalyzeResultDto readResult(String redisKey, String requestId) {
        // StringRedisSerializer 로 저장된 평문 JSON 문자열을 읽는다.
        Object raw = redisService.getValues(redisKey);
        if (raw == null) {
            // 아직 결과 없음(분석 진행 전/키 만료) → 404
            throw new AnalysisResultNotFoundException();
        }

        JsonNode root = parse(raw.toString(), requestId);
        String status = root.path("status").asText("");
        if (!STATUS_DONE.equals(status)) {
            // processing / failed / 알 수 없음 → 404 (프론트가 계속 폴링)
            log.debug("[getAnalyzeResult] 아직 완료 아님: requestId={}, status={}", requestId, status);
            throw new AnalysisResultNotFoundException();
        }

        JsonNode resultNode = root.get("result");
        if (resultNode == null || resultNode.isNull()) {
            log.error("[getAnalyzeResult] done 이지만 result 없음: requestId={}", requestId);
            throw new AnalysisResultNotFoundException();
        }

        return convert(resultNode, requestId);
    }

    private JsonNode parse(String json, String requestId) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            log.error("[getAnalyzeResult] Redis 문서 파싱 실패: requestId={}, message={}", requestId, e.getMessage());
            throw new AnalysisResultNotFoundException();
        }
    }

    private AnalyzeResultDto convert(JsonNode resultNode, String requestId) {
        try {
            return OBJECT_MAPPER.treeToValue(resultNode, AnalyzeResultDto.class);
        } catch (Exception e) {
            log.error("[getAnalyzeResult] result 변환 실패: requestId={}, message={}", requestId, e.getMessage());
            throw new AnalysisResultNotFoundException();
        }
    }
}
