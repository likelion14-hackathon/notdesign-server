package com.likelionknu.notdesign.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// analyze FastAPI의 202 즉시 응답(request_id, redis_key).
public record AnalyzeAcceptedDto(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("redis_key") String redisKey
) {
}
