package com.likelionknu.notdesign.result.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultNotFoundException extends GlobalException {
    public ResultNotFoundException(Long planId) {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[ResultNotFoundException] 플랜의 기준선 측정 없음: planId={}", planId);
    }
}
