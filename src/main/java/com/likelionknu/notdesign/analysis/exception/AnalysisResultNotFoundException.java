package com.likelionknu.notdesign.analysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

// 결과가 아직 없거나 done이 아닐 때 → 404. 프론트는 200이 올 때까지 폴링한다.
public class AnalysisResultNotFoundException extends GlobalException {
    public AnalysisResultNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
