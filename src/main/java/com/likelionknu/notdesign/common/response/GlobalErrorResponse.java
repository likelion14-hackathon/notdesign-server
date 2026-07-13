package com.likelionknu.notdesign.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GlobalErrorResponse {
    private final String code;
    private final String message;

    public static GlobalErrorResponse from(ErrorCode errorCode) {
        return GlobalErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static GlobalErrorResponse empty() {
        return new GlobalErrorResponse(null, null);
    }
}