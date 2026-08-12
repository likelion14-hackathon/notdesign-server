package com.likelionknu.notdesign.skinanalysis.client;

import com.likelionknu.notdesign.skinanalysis.client.dto.AnalyzeAcceptedDto;
import com.likelionknu.notdesign.skinanalysis.client.dto.AnalyzeRequestDto;
import com.likelionknu.notdesign.skinanalysis.exception.AnalyzeServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * analyze FastAPI 서버(기본 http://localhost:8000) 호출 전용 클라이언트.
 * POST /analyze 로 이미지 분석을 시작시키고, 202 응답(request_id/redis_key)을 받는다.
 * 실제 분석은 서버가 백그라운드로 수행하고 결과는 클라우드 Redis 에 저장한다.
 */
@Slf4j
@Component
public class AnalyzeClient {
    private final RestClient restClient = RestClient.create();

    @Value("${services.analyze.url}")
    private String analyzeUrl;

    /**
     * 피부 이미지 분석 요청(POST /analyze).
     *
     * @param imageUrl 공개 접근 가능한 얼굴 이미지 URL
     * @return 202 응답 DTO(request_id, redis_key)
     */
    public AnalyzeAcceptedDto requestAnalyze(String imageUrl) {
        return post("/analyze", imageUrl);
    }

    private AnalyzeAcceptedDto post(String path, String imageUrl) {
        try {
            AnalyzeAcceptedDto response = restClient.post()
                    .uri(analyzeUrl + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AnalyzeRequestDto.of(imageUrl))
                    .retrieve()
                    .body(AnalyzeAcceptedDto.class);

            if (response == null || response.requestId() == null) {
                log.error("[AnalyzeClient] 분석 서버 응답이 비정상입니다: path={}", path);
                throw new AnalyzeServerException();
            }
            return response;
        } catch (RestClientException e) {
            log.error("[AnalyzeClient] 분석 서버 호출 실패: path={}, message={}", path, e.getMessage());
            throw new AnalyzeServerException();
        }
    }
}
