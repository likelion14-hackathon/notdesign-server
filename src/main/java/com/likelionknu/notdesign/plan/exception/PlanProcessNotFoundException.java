package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanProcessNotFoundException extends GlobalException {
    public PlanProcessNotFoundException(Long userId) {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[PlanProcessNotFoundException] 진행 중인 사이클 없음: userId={}", userId);
    }
}
