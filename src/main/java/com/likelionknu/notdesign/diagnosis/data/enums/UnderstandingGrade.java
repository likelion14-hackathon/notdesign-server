package com.likelionknu.notdesign.diagnosis.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UnderstandingGrade {
    HIGH("높음"),
    MEDIUM("보통"),
    LOW("낮음"),
    VERY_LOW("매우 낮음");

    private final String displayName;

    public static UnderstandingGrade from(int score) {
        if (score >= 12) {
            return HIGH;
        }
        if (score >= 11) {
            return MEDIUM;
        }
        if (score >= 5) {
            return LOW;
        }
        return VERY_LOW;
    }
}
