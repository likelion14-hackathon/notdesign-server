package com.likelionknu.notdesign.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("C001", "잘못된 요청입니다.", 400),
    INVALID_PARAMETER("C002", "유효하지 않은 파라미터입니다.", 400),
    VERIFICATION_INVALID("C4011", "인증 정보가 유효하지 않습니다.", 401),
    PASSWORD_INVALID("C4012", "비밀번호가 일치하지 않습니다.", 401),
    TOKEN_INVALID("C4013", "유효하지 않은 토큰입니다.", 401),
    ACCESS_DENIED("C403", "승인되지 않은 사용자입니다.", 403),
    USER_DATA_NOT_FOUND("C4041", "사용자를 찾을 수 없습니다.", 404),
    DATA_NOT_FOUND("C404", "정보를 불러올 수 없습니다.", 404),
    EMAIL_DUPLICATION("C4091", "이미 존재하는 이메일입니다.", 409),
    UNKNOWN_ERROR("C500", "오류가 발생하였습니다.", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}