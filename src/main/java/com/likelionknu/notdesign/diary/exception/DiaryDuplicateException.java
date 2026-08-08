package com.likelionknu.notdesign.diary.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;

@Slf4j
public class DiaryDuplicateException extends GlobalException {
    public DiaryDuplicateException(Long userId, LocalDate recordedDate) {
        super(ErrorCode.INVALID_REQUEST);
        log.error("[DiaryDuplicateException] 이미 기록한 사용자: userId={}, date={}", userId, recordedDate);
    }
}
