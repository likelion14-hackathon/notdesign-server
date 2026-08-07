package com.likelionknu.notdesign.diary.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;

@Slf4j
public class DiaryNotFoundException extends GlobalException {
    public DiaryNotFoundException(Long userId, LocalDate recordedDate) {
        super(ErrorCode.DATA_NOT_FOUND);
        log.error("[DiaryNotFoundException] 기록 미존재: userId={}, date={}", userId, recordedDate);
    }
}
