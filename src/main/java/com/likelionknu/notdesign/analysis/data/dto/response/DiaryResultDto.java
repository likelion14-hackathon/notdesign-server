package com.likelionknu.notdesign.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record DiaryResultDto(
        @JsonProperty("skin_tone") double skinTone,
        double pores,
        double redness,
        Map<String, Double> confidence
) {
}
