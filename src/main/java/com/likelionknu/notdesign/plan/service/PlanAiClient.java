package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse;
import com.likelionknu.notdesign.plan.exception.PlanGenerationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanAiClient {
    private static final String MODEL = "gpt-5.4";

    private final ChatClient chatClient;

    public PlanAiClient(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().model(MODEL))
                .build();
    }

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
