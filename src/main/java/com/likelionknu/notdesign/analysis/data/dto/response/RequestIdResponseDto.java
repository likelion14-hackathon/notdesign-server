package com.likelionknu.notdesign.analysis.data.dto.response;

public record RequestIdResponseDto(
        String requestId
) {
    public static RequestIdResponseDto of(String requestId) {
        return new RequestIdResponseDto(requestId);
    }
}
