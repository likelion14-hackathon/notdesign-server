package com.likelionknu.notdesign.report.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportMetricResponseDto {
    private ImprovementItem improvement;
    private String improvementName;
    private Integer before;
    private Integer after;
    private Integer delta;
}
