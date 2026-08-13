package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse;
import com.likelionknu.notdesign.plan.exception.PlanGenerationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanAiClient {
    private final ChatClient chatClient;

    public PlanAiClient(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 프롬프트를 보내 플랜 항목을 생성한다.
     *
     * @param systemPrompt 규칙 + 카탈로그가 담긴 시스템 프롬프트
     * @param userPrompt   모드·측정값 등이 담긴 사용자 프롬프트
     * @return 검증 전 AI 응답(planSummary + items)
     * @throws PlanGenerationFailedException 호출 실패 또는 응답이 비어 있는 경우
     */
    public PlanGenerationAiResponse generate(String systemPrompt, String userPrompt) {
        try {
            PlanGenerationAiResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(PlanGenerationAiResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                throw new PlanGenerationFailedException("AI 응답이 비어 있음");
            }

            return response;
        } catch (PlanGenerationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new PlanGenerationFailedException(e.getMessage());
        }
    }
}
