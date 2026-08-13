package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanGenerationFailedException extends GlobalException {
    public PlanGenerationFailedException(String reason) {
        super(ErrorCode.PLAN_GENERATION_FAILED);
        log.error("[PlanGenerationFailedException] AI 플랜 생성 실패: {}", reason);
    }
}
