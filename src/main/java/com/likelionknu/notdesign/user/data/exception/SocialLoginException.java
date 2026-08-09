package com.likelionknu.notdesign.user.data.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

/**
 * 소셜 로그인(카카오 등) 과정에서 토큰 교환 또는 사용자 조회에 실패했을 때 발생한다.
 */
public class SocialLoginException extends GlobalException {
    public SocialLoginException() {
        super(ErrorCode.SOCIAL_LOGIN_FAILED);
    }
}
