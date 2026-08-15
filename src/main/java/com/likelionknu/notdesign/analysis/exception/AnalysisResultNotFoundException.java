package com.likelionknu.notdesign.analysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

public class AnalysisResultNotFoundException extends GlobalException {
    public AnalysisResultNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
