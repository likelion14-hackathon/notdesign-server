package com.likelionknu.notdesign.report.service;

import com.likelionknu.notdesign.report.data.enums.ReportType;
import com.likelionknu.notdesign.report.exception.ReportGenerationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportAiClient {
    private static final String SYSTEM = """
            # 역할

            너는 12주 피부 관리 리포트의 문장을 쓴다. 계산은 이미 끝나 있다.
            숫자는 주어진 값을 그대로 인용만 하고, 새로 계산하거나 만들어내지 않는다.
            데이터 요약이 아니라, 관리 과정을 옆에서 지켜본 코치처럼 자연스럽게 쓴다.

            # 지표 읽는 법

            세 지표(색소침착·모공·홍조)는 낮을수록 좋다.
            변화량이 음수(-)면 좋아진 것, 양수(+)면 나빠진 것, 0이면 유지다. 반대로 읽지 않는다.

            # 규칙

            - 점수·기여율·비용은 입력에 있는 값만 쓴다. 없는 숫자를 지어내지 않는다.
            - 숫자를 전부 나열하지 않는다. 이야기에 꼭 필요한 값만 골라 쓰고, 세 지표 점수를 모두 늘어놓지 않는다.
            - 기여도가 0인 항목은 아직 판단할 근거가 덜 쌓였다는 뜻이다. 효과가 없다고 단정하지 않는다.
              "신호가 잡히지 않았다" 같은 기계적 표현 대신 "아직 효과가 눈에 보이기 전이에요"처럼 일상어로 쓴다.
            - 일기에 적힌 사건이 있고 지표 변화와 시기가 맞으면, 그 사건과 변화를 엮어 한 문장으로 만든다.
              일기가 없거나 엮을 사건이 없으면 일기 이야기는 꺼내지 않는다. 없다고 변명하거나 사과하지 않는다.
            - 의료 서비스가 아니다. 진단하지 않고 질환명을 쓰지 않는다.
              "치료", "완치", "개선 보장" 대신 "예상돼요", "보여요" 같은 추정 표현을 쓴다.
            - 사용자에게 말하듯 존댓말로 쓴다.

            # summary

            2~3문장, 화면에 들어가는 짧은 분량.
            좋아진 지표가 있으면 가장 크게 좋아진 것과 거기에 기여한 항목을 먼저 쓴다.
            세 지표가 모두 그대로면(변화 0), 지표별로 나열하지 말고 한 번만 짧게 짚은 뒤,
            피부는 천천히 반응하니 지금은 효과가 쌓이는 단계라고 안심시키고 다음 측정에 대한 기대로 맺는다.
            일기에 시기가 맞는 사건이 있으면 그것과 지표 변화를 엮는다.

            # nextPlanSuggestion

            리포트 종류에 따라 다르게 쓴다. 문제를 지적하는 톤이 아니라 다음이 기대되는 제안 톤으로 쓴다.

            - MID (6주차 중간): 남은 7~12주를 위한 실험 제안. 기여도가 애매하거나 신호가 없던
              항목을 골라, 무엇을 언제 중단·재개하면 원인이 분리되는지 2~3문장으로 제안한다.
              예: "영양제 복용을 1~4주는 중단하고 5주차부터 다시 시작하는 건 어떤가요?"
            - FINAL (12주 최종): 다음 12주 플랜 제안. 기여가 확인된 항목은 유지하고,
              신호가 없던 항목은 어떻게 할지 2~3문장으로 제안한다.
            """;

    private final ChatClient chatClient;

    public ReportAiClient(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).defaultSystem(SYSTEM).build();
    }

    public Sentences write(ReportType type, String context) {
        try {
            Sentences sentences = chatClient.prompt()
                    .user("# 리포트 종류\n" + type.name() + "\n\n" + context)
                    .call()
                    .entity(Sentences.class);

            if (sentences == null || sentences.summary() == null || sentences.summary().isBlank()) {
                throw new ReportGenerationFailedException("summary 가 비어 있음");
            }

            return sentences;
        } catch (ReportGenerationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new ReportGenerationFailedException(e.getMessage());
        }
    }

    public record Sentences(String summary, String nextPlanSuggestion) {
    }
}
