package com.likelionknu.notdesign.report.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportNotFoundException extends GlobalException {
    public ReportNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[ReportNotFoundException] 리포트 조회 실패");
    }

    public ReportNotFoundException(Long reportId) {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[ReportNotFoundException] 리포트 조회 실패: reportId={}", reportId);
    }
}
