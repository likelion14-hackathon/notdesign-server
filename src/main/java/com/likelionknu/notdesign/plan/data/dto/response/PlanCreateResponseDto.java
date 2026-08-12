package com.likelionknu.notdesign.plan.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.PlanGenerationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateResponseDto {
    private Long planId;
    private PlanGenerationMode mode;
    private String planSummary;
    private Integer durationWeeks;
    private Integer totalPrice;
    private List<PlanDetailItemResponseDto> items;
}
