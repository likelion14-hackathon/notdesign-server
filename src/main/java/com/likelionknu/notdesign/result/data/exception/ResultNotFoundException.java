package com.likelionknu.notdesign.result.data.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

public class ResultNotFoundException extends GlobalException {
    public ResultNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
