package com.likelionknu.notdesign.clinic.data.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

/**
 * 요청한 클리닉을 찾을 수 없을 때 발생한다.
 */
public class ClinicNotFoundException extends GlobalException {
    public ClinicNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
