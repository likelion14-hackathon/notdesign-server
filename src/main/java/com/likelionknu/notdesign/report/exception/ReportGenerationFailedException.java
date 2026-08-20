package com.likelionknu.notdesign.report.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportGenerationFailedException extends GlobalException {
    public ReportGenerationFailedException(String reason) {
        super(ErrorCode.REPORT_GENERATION_FAILED);
        log.error("[ReportGenerationFailedException] 리포트 문장 생성 실패: {}", reason);
    }
}
