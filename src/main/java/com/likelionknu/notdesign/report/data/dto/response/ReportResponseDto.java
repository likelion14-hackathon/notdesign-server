package com.likelionknu.notdesign.report.data.dto.response;

import com.likelionknu.notdesign.report.data.enums.ReportType;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {
    private Long reportId;
    private ReportType type;
    private String summary;
    private String nextPlanSuggestion;
    private Integer nextPlanPrice;

    private List<ReportMetricResponseDto> metrics;
    private List<ReportContributionResponseDto> contributions;
}
