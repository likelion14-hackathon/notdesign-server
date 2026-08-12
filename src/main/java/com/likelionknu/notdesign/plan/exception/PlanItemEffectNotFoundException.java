package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanItemEffectNotFoundException extends GlobalException {
    public PlanItemEffectNotFoundException(String name) {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[PlanItemEffectNotFoundException] item_effect 에 이름이 없어 가중치를 찾을 수 없음: name={}", name);
    }
}
