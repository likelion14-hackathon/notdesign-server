package com.likelionknu.notdesign.report.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportAccessDeniedException extends GlobalException {
    public ReportAccessDeniedException(Long userId, Long reportId) {
        super(ErrorCode.ACCESS_DENIED);
        log.error("[ReportAccessDeniedException] 다른 사용자의 리포트 접근: userId={}, reportId={}", userId, reportId);
    }
}
