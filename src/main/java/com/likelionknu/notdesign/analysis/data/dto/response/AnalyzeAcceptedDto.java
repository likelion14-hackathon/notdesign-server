package com.likelionknu.notdesign.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnalyzeAcceptedDto(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("redis_key") String redisKey
) {
}
