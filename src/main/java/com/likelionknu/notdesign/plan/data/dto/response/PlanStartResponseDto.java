package com.likelionknu.notdesign.plan.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStartResponseDto {
    private Long processId;
    private LocalDate startedAt;
}
