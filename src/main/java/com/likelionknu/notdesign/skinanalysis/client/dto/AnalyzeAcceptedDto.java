package com.likelionknu.notdesign.skinanalysis.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * analyze FastAPI 의 202 즉시 응답. 필드는 request_id, redis_key 두 개뿐(status/version 없음).
 */
public record AnalyzeAcceptedDto(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("redis_key") String redisKey
) {
}
