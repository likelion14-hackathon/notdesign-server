package com.likelionknu.notdesign.analysis.data.dto.response;

import java.util.Map;

// analyze 결과(0~100). confidence는 지표별(pigmentation/erythema/hydration) 신뢰도.
public record AnalyzeResultDto(
        double pigmentation,
        double erythema,
        double hydration,
        Map<String, Double> confidence
) {
}
