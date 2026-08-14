package com.likelionknu.notdesign.analysis.data.dto.response;

// 분석 요청 수락 후 반환. 프론트는 이 requestId로 결과를 폴링한다.
public record RequestIdResponseDto(
        String requestId
) {
    public static RequestIdResponseDto of(String requestId) {
        return new RequestIdResponseDto(requestId);
    }
}
