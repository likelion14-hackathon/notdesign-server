package com.likelionknu.notdesign.plan.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.plan.data.dto.response.PlanDetailResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanStatsResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanSummaryResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanTodoResponseDto;
import com.likelionknu.notdesign.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    @GetMapping("/current")
    @Operation(summary = "현재 플랜 진행 요약 조회")
    public GlobalResponse<PlanSummaryResponseDto> getCurrentPlanSummary() {
        return GlobalResponse.ok(planService.getCurrentPlanSummary(SecurityUtil.getUsername()));
    }

    @GetMapping("/current/detail")
    @Operation(summary = "현재 플랜 상세 조회")
    public GlobalResponse<PlanDetailResponseDto> getCurrentPlanDetail() {
        return GlobalResponse.ok(planService.getCurrentPlanDetail(SecurityUtil.getUsername()));
    }

    @GetMapping("/current/stats")
    @Operation(summary = "현재 플랜 기록 통계 조회")
    public GlobalResponse<PlanStatsResponseDto> getCurrentPlanStats() {
        return GlobalResponse.ok(planService.getCurrentPlanStats(SecurityUtil.getUsername()));
    }

    @GetMapping("/current/todos")
    @Operation(summary = "오늘 체크리스트 조회")
    public GlobalResponse<List<PlanTodoResponseDto>> getCurrentPlanTodos() {
        return GlobalResponse.ok(planService.getCurrentPlanTodos(SecurityUtil.getUsername()));
    }
}
