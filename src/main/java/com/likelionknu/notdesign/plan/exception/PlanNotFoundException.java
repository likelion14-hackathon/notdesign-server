package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanNotFoundException extends GlobalException {
    public PlanNotFoundException(Long planId) {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[PlanNotFoundException] 플랜 없음: planId={}", planId);
    }
}
