package com.likelionknu.notdesign.report.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import com.likelionknu.notdesign.report.data.enums.Reliability;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportContributionResponseDto {
    private ImprovementItem improvement;
    private String improvementName;
    private PlanCategory category;
    private String categoryName;
    private String name;
    private BigDecimal score;
    private BigDecimal contributionRate;
    private Integer price;
    private Integer costPerPoint;
    private Reliability reliability;
}
