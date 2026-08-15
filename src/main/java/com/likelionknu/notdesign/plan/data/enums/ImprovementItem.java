package com.likelionknu.notdesign.plan.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImprovementItem {
    PIGMENTATION("색소침착"),
    ERYTHEMA("홍조"),
    PORES("모공"),

    /**
     * @deprecated 지표 변경으로 인한 삭제 예정
     */
    @Deprecated
    HYDRATION("수분력");

    private final String displayName;
}
