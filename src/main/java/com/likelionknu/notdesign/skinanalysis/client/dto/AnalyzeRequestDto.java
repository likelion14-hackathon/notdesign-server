package com.likelionknu.notdesign.skinanalysis.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spring → analyze FastAPI 요청 바디.
 * FastAPI 는 snake_case 이므로 @JsonProperty 로 매핑한다.
 * referenceBbox 는 그레이패치가 있을 때만 사용, 보통 null 이라 직렬화에서 제외한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyzeRequestDto(
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("reference_bbox") int[] referenceBbox
) {
    public static AnalyzeRequestDto of(String imageUrl) {
        return new AnalyzeRequestDto(imageUrl, null);
    }
}
