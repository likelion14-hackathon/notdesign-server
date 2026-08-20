package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.common.ai.AiModelFallbackExecutor;
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
    private final AiModelFallbackExecutor fallbackExecutor;

    public PlanAiClient(ChatModel chatModel, AiModelFallbackExecutor fallbackExecutor) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.fallbackExecutor = fallbackExecutor;
    }

    public PlanGenerationAiResponse generate(String systemPrompt, String userPrompt) {
        try {
            PlanGenerationAiResponse response = fallbackExecutor.execute(options -> chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(options)
                    .call()
                    .entity(PlanGenerationAiResponse.class));

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
