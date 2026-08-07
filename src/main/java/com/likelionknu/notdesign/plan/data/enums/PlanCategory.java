package com.likelionknu.notdesign.plan.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanCategory {
    PROCEDURE("시술"),
    SUPPLEMENT("영양제"),
    HOME_CARE("홈케어"),
    LIFESTYLE("생활습관");

    private final String displayName;
}
