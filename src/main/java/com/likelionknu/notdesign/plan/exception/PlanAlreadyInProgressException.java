package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanAlreadyInProgressException extends GlobalException {
    public PlanAlreadyInProgressException(Long userId) {
        super(ErrorCode.PLAN_ALREADY_IN_PROGRESS);
        log.error("[PlanAlreadyInProgressException] 이미 진행 중인 플랜 존재: userId={}", userId);
    }
}
