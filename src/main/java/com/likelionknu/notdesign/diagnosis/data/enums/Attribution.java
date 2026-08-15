package com.likelionknu.notdesign.diagnosis.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Attribution {
    KNOWN("%단위로 잘 알고 있다", 10, 10),
    GUESSED("감과 추측으로 안다", 4, 30),
    UNKNOWN("잘 모른다", 0, 50);

    private final String displayName;
    private final int score;
    private final int utilLeak;
}
