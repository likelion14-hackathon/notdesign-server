package com.likelionknu.notdesign.analysis.data.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyzeRequestDto(
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("reference_bbox") int[] referenceBbox
) {
    public static AnalyzeRequestDto of(String imageUrl) {
        return new AnalyzeRequestDto(imageUrl, null);
    }
}
