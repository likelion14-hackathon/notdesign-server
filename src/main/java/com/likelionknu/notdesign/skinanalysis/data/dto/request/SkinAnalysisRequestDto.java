package com.likelionknu.notdesign.skinanalysis.data.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 프론트 → Spring 피부 이미지 분석 요청 바디.
 * imageUrl 은 S3 등에 업로드되어 공개 접근 가능한 얼굴 이미지 URL.
 */
public record SkinAnalysisRequestDto(
        @NotBlank(message = "imageUrl은 필수입니다.")
        String imageUrl
) {
}
