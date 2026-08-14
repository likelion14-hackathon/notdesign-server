package com.likelionknu.notdesign.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

// diary(체험) 결과(0~10). confidence는 지표별(skin_tone/dryness/redness) 신뢰도.
public record DiaryResultDto(
        @JsonProperty("skin_tone") double skinTone,
        double dryness,
        double redness,
        Map<String, Double> confidence
) {
}
