package com.likelionknu.notdesign.analysis.data.dto.response;

import java.util.Map;

public record AnalyzeResultDto(
        double pigmentation,
        double erythema,
        double hydration,
        Map<String, Double> confidence
) {
}
