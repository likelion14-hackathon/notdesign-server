package com.likelionknu.notdesign.analysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

public class AnalyzeServerException extends GlobalException {
    public AnalyzeServerException() {
        super(ErrorCode.ANALYZE_SERVER_ERROR);
    }
}
