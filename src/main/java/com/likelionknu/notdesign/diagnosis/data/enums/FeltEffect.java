package com.likelionknu.notdesign.diagnosis.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeltEffect {
    HIGH("많이 체감했다", 3, 0),
    MEDIUM("보통 수준이었다", 2, 15),
    LOW("거의 체감하지 못했다", 1, 30);

    private final String displayName;
    private final int score;
    private final int baseLeak;
}
