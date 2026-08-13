package com.likelionknu.notdesign.plan.data.dto.ai;

import java.util.List;

public record PlanGenerationAiResponse(String planSummary, List<Item> items) {

    public record Item(Integer itemEffectId, String frequency, List<Integer> weeks, String reason) {
    }
}
