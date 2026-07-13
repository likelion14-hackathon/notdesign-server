package com.likelionknu.notdesign.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GlobalResponse<T> {
    private final T data;
    private final GlobalErrorResponse error;

    public static <T> GlobalResponse<T> ok(T data) {
        return new GlobalResponse<>(data, GlobalErrorResponse.empty());
    }

    public static GlobalResponse<Void> ok() {
        return new GlobalResponse<>(null, GlobalErrorResponse.empty());
    }

    public static <T> GlobalResponse<T> error(ErrorCode errorCode) {
        return new GlobalResponse<>(null, GlobalErrorResponse.from(errorCode));
    }

    public static <T> GlobalResponse<T> error(GlobalException e) {
        return error(e.getErrorCode());
    }
}