package com.likelionknu.notdesign.skinanalysis.data.dto.response;

import java.util.Map;

/**
 * analyze 결과(0~100 스케일). Redis 문서의 result 필드를 그대로 파싱해 프론트에 반환한다.
 * confidence 는 각 지표별 신뢰도(pigmentation/erythema/hydration).
 */
public record AnalyzeResultDto(
        double pigmentation,
        double erythema,
        double hydration,
        Map<String, Double> confidence
) {
}
