package com.likelionknu.notdesign.analysis.data.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// Spring → analyze FastAPI 요청 바디. snake_case 매핑, referenceBbox는 보통 null이라 직렬화 제외.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyzeRequestDto(
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("reference_bbox") int[] referenceBbox
) {
    public static AnalyzeRequestDto of(String imageUrl) {
        return new AnalyzeRequestDto(imageUrl, null);
    }
}
