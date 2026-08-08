package com.likelionknu.notdesign.plan.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDetailResponseDto {
    private String planSummary;
    private Integer totalPrice;
    private Metrics metrics;
    private List<PlanDetailItemResponseDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private Integer pigmentation;
        private Integer hydration;
        private Integer erythema;
    }
}
