package com.likelionknu.notdesign.report.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecutionResponseDto {
    private PlanCategory category;
    private String categoryName;
    private String name;
    private List<Integer> plannedWeeks;
    private List<Integer> doneWeeks;
}
