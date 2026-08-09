package com.likelionknu.notdesign.clinic.data.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

public class ClinicNotFoundException extends GlobalException {
    public ClinicNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
