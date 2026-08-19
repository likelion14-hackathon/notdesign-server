package com.likelionknu.notdesign.plan.data.enums;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImprovementItem {
    PIGMENTATION("색소침착"),
    ERYTHEMA("홍조"),
    PORES("모공");

    private final String displayName;

    /**
     * 폐기되지 않은(@Deprecated 미표시) 지표만 반환합니다.
     */
    public static List<ImprovementItem> activeValues() {
        return Arrays.stream(values())
                .filter(item -> !item.isDeprecated())
                .toList();
    }

    private boolean isDeprecated() {
        try {
            return ImprovementItem.class.getField(name()).isAnnotationPresent(Deprecated.class);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
