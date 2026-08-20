package com.likelionknu.notdesign.plan.data.dto.request;

import com.likelionknu.notdesign.plan.data.enums.PlanGenerationMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequestDto {

    @NotNull
    private PlanGenerationMode mode;

    private Integer monthlyBudget;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double skinTone;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double pores;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double redness;
}
