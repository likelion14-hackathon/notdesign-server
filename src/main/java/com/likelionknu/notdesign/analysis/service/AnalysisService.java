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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AnalyzeClient analyzeClient;
    private final S3Uploader s3Uploader;
    private final RedisService redisService;

    /**
     * 이미지를 S3에 업로드한 뒤 피부 이미지 분석을 요청합니다.
     *
     * @param image 분석할 얼굴 이미지 파일
     * @return 결과 조회에 사용할 requestId
     */
    public String requestAnalyze(MultipartFile image) {
        String imageUrl = s3Uploader.upload(image);
        AnalyzeAcceptedDto accepted = analyzeClient.requestAnalyze(imageUrl);
        log.info("[requestAnalyze] 분석 요청 수락: requestId={}", accepted.requestId());
        return accepted.requestId();
    }

    /**
     * 피부 이미지 분석 결과를 조회합니다. 분석이 완료되지 않았으면 조회할 수 없습니다.
     *
     * @param requestId 분석 요청 식별자
     * @return 피부 이미지 분석 결과
     */
    public AnalyzeResultDto getAnalyzeResult(String requestId) {
        return readResult(requestId + ":analyze", requestId, AnalyzeResultDto.class);
    }

    /**
     * 이미지를 S3에 업로드한 뒤 체험 피부 이미지 분석을 요청합니다.
     *
     * @param image 분석할 얼굴 이미지 파일
     * @return 결과 조회에 사용할 requestId
     */
    public String requestDiary(MultipartFile image) {
        String imageUrl = s3Uploader.upload(image);
        AnalyzeAcceptedDto accepted = analyzeClient.requestDiary(imageUrl);
        log.info("[requestDiary] 체험 분석 요청 수락: requestId={}", accepted.requestId());
        return accepted.requestId();
    }

    /**
     * 체험 피부 이미지 분석 결과를 조회합니다. 분석이 완료되지 않았으면 조회할 수 없습니다.
     *
     * @param requestId 분석 요청 식별자
     * @return 체험 피부 이미지 분석 결과
     */
    public DiaryResultDto getDiaryResult(String requestId) {
        return readResult(requestId + ":diary", requestId, DiaryResultDto.class);
    }

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
