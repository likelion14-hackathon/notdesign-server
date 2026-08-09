package com.likelionknu.notdesign.result.data.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

/**
 * 측정 결과(또는 복사할 더미 측정값)를 찾을 수 없을 때 발생한다.
 */
public class ResultNotFoundException extends GlobalException {
    public ResultNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
