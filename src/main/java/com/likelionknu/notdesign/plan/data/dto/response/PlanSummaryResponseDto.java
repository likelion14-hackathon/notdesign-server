package com.likelionknu.notdesign.plan.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanSummaryResponseDto {
    private int currentWeek;
    private int totalWeeks;
    private int progressRate;
    private int daysToMidReport;
    private int daysToFinalReport;
}
