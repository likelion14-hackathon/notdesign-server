package com.likelionknu.notdesign.diary.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.diary.data.dto.response.DiaryResponseDto;
import com.likelionknu.notdesign.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;

    @GetMapping("/{recordedDate}")
    @Operation(summary = "특정 일자 기록 조회")
    public GlobalResponse<DiaryResponseDto> getDiary(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate recordedDate) {
        return GlobalResponse.ok(diaryService.getDiary(SecurityUtil.getUsername(), recordedDate));
    }
}
