package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanAlreadyStartedException extends GlobalException {
    public PlanAlreadyStartedException(Long planId) {
        super(ErrorCode.PLAN_ALREADY_STARTED);
        log.error("[PlanAlreadyStartedException] 이미 시작된 플랜 삭제 시도: planId={}", planId);
    }
}
