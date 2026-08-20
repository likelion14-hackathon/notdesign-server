package com.likelionknu.notdesign.analysis.client;

import com.likelionknu.notdesign.analysis.data.dto.request.AnalyzeRequestDto;
import com.likelionknu.notdesign.analysis.data.dto.response.AnalyzeAcceptedDto;
import com.likelionknu.notdesign.analysis.exception.AnalyzeServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class AnalyzeClient {
    private final RestClient restClient = RestClient.create();

    @Value("${services.analyze.url}")
    private String analyzeUrl;

    public AnalyzeAcceptedDto requestAnalyze(String imageUrl) {
        return post("/analyze", imageUrl);
    }

    public AnalyzeAcceptedDto requestDiary(String imageUrl) {
        return post("/analyze/diary", imageUrl);
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
