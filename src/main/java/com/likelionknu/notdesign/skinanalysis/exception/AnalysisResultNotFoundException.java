package com.likelionknu.notdesign.skinanalysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

/**
 * 분석 결과가 아직 없거나(키 없음/processing/failed) done 이 아닐 때 → 404.
 * 프론트는 로딩 화면에서 200 이 올 때까지 폴링한다.
 */
public class AnalysisResultNotFoundException extends GlobalException {
    public AnalysisResultNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
