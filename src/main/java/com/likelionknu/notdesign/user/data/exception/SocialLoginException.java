package com.likelionknu.notdesign.user.data.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

public class SocialLoginException extends GlobalException {
    public SocialLoginException() {
        super(ErrorCode.SOCIAL_LOGIN_FAILED);
    }
}
