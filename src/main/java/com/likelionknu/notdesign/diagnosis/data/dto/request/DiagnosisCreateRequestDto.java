package com.likelionknu.notdesign.diagnosis.data.dto.request;

import com.likelionknu.notdesign.diagnosis.data.enums.Attribution;
import com.likelionknu.notdesign.diagnosis.data.enums.FeltEffect;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisCreateRequestDto {
    @NotNull
    @Min(0)
    @Max(1000000)
    private Integer procedureCost;

    @NotNull
    @Min(0)
    @Max(1000000)
    private Integer productCost;

    @NotNull
    private FeltEffect feltEffect;

    @NotNull
    private Attribution attribution;
}
