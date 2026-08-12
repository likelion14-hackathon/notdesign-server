package com.likelionknu.notdesign.plan.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanCycleNotFinishedException extends GlobalException {
    public PlanCycleNotFinishedException(Long processId, int currentWeek, int totalWeeks) {
        super(ErrorCode.PLAN_CYCLE_NOT_FINISHED);
        log.error("[PlanCycleNotFinishedException] 사이클이 끝나지 않음: processId={}, {}/{}주",
                processId, currentWeek, totalWeeks);
    }
}
