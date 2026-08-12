package com.likelionknu.notdesign.skinanalysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

/**
 * analyze FastAPI 호출 자체가 실패했을 때(네트워크/5xx/응답 파싱 실패 등).
 */
public class AnalyzeServerException extends GlobalException {
    public AnalyzeServerException() {
        super(ErrorCode.ANALYZE_SERVER_ERROR);
    }
}
