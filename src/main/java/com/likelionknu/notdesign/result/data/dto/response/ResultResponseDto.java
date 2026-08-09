package com.likelionknu.notdesign.result.data.dto.response;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.result.data.entity.Result;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResultResponseDto {
    private final Long id;
    private final Long clinicId;
    private final String clinicName;
    private final Long planId;
    private final Integer pigmentation;
    private final Integer erythema;
    private final Integer hydration;
    private final LocalDateTime measuredAt;
    private final LocalDateTime createdAt;

    public static ResultResponseDto from(Result result) {
        Clinic clinic = result.getClinic();
        Plan plan = result.getPlan();

        return ResultResponseDto.builder()
                .id(result.getId())
                .clinicId(clinic != null ? clinic.getId() : null)
                .clinicName(clinic != null ? clinic.getName() : null)
                .planId(plan != null ? plan.getId() : null)
                .pigmentation(result.getPigmentation())
                .erythema(result.getErythema())
                .hydration(result.getHydration())
                .measuredAt(result.getMeasuredAt())
                .createdAt(result.getCreatedAt())
                .build();
    }
}
