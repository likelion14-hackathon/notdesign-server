package com.likelionknu.notdesign.plan.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanAdjustResponseDto {
    private Long planId;
    private Integer currentWeek;
}
