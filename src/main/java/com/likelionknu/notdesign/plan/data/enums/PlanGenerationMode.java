package com.likelionknu.notdesign.plan.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanGenerationMode {
    NEW(PlanType.FULL, 12),
    TRIAL(PlanType.TRIAL, 1),
    NEXT(PlanType.FULL, 12),
    ADJUST(PlanType.FULL, 12);

    private final PlanType planType;
    private final int durationWeeks;
}
