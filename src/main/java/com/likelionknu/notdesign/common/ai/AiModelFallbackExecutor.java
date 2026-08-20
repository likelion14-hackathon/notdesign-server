package com.likelionknu.notdesign.common.ai;

import com.openai.errors.OpenAIServiceException;
import com.openai.errors.RateLimitException;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * 설정된 모델을 성능 순서대로 시도하는 폴백 실행기.
 *
 * <p>어떤 모델이 하루 한도(429)에 걸리거나 사용할 수 없으면 다음 모델로 넘어가고,
 * 모든 모델이 실패하면 마지막 예외를 그대로 던진다. 단 계정 크레딧 소진
 * (insufficient_quota)은 모델을 바꿔도 해결되지 않으므로 즉시 실패시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelFallbackExecutor {
    private static final String INSUFFICIENT_QUOTA = "insufficient_quota";

    private final AiFallbackProperties properties;

    /**
     * 전달받은 호출을 폴백 모델 목록으로 순차 실행한다.
     *
     * @param call 모델 옵션 빌더를 받아 AI 호출을 수행하는 함수
     * @return 가장 먼저 성공한 모델의 응답
     */
    public <T> T execute(Function<OpenAiChatOptions.Builder, T> call) {
        List<String> models = properties.getModels();
        if (models.isEmpty()) {
            return call.apply(OpenAiChatOptions.builder());
        }

        OpenAIServiceException lastError = null;

        for (String model : models) {
            try {
                return call.apply(OpenAiChatOptions.builder().model(model));
            } catch (RateLimitException e) {
                if (INSUFFICIENT_QUOTA.equals(e.code().orElse(null))) {
                    log.error("[AI 폴백] 계정 크레딧 소진(insufficient_quota) — 폴백 중단");
                    throw e;
                }
                log.warn("[AI 폴백] {} 한도 초과(429) → 다음 모델", model);
                lastError = e;
            } catch (OpenAIServiceException e) {
                log.warn("[AI 폴백] {} 사용 불가 → 다음 모델: {}", model, e.getMessage());
                lastError = e;
            }
        }

        log.error("[AI 폴백] 모든 모델 소진 — 생성 실패");
        throw lastError;
    }
}
