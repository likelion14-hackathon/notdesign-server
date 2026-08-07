package com.likelionknu.notdesign.plan.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImprovementItem {
    PIGMENTATION("색소침착"),
    ERYTHEMA("홍조"),
    HYDRATION("수분력");

    private final String displayName;
}
