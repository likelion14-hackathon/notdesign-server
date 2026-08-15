package com.likelionknu.notdesign.diagnosis.data.dto.response;

import com.likelionknu.notdesign.diagnosis.data.enums.UnderstandingGrade;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponseDto {
    private Long diagnosisId;
    private Integer totalCost;
    private Integer monthlyAverage;
    private UnderstandingGrade grade;
    private String gradeName;
    private Integer wasteRate;
    private Integer wasteAmount;
    private String problemDiagnosis;
    private String wasteDescription;
}
