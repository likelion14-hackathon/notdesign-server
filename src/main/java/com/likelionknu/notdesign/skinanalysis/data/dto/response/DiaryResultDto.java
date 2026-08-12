package com.likelionknu.notdesign.skinanalysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * diary(체험) 결과(0~10 스케일). Redis 문서의 result 필드를 그대로 파싱해 프론트에 반환한다.
 * confidence 는 각 지표별 신뢰도(skin_tone/dryness/redness).
 */
public record DiaryResultDto(
        @JsonProperty("skin_tone") double skinTone,
        double dryness,
        double redness,
        Map<String, Double> confidence
) {
}
