package com.likelionknu.notdesign.report.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultNotImportedException extends GlobalException {
    public ResultNotImportedException(Long userId) {
        super(ErrorCode.RESULT_ALREADY_IMPORTED);
        log.error("[ResultNotImportedException] 새로 불러올 측정 데이터 없음: userId={}", userId);
    }
}
