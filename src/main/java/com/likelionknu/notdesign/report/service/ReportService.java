package com.likelionknu.notdesign.report.service;

import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.repository.PlanProcessRepository;
import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.report.data.dto.response.ReportContributionResponseDto;
import com.likelionknu.notdesign.report.data.dto.response.ReportExecutionResponseDto;
import com.likelionknu.notdesign.report.data.dto.response.ReportMetricResponseDto;
import com.likelionknu.notdesign.report.data.dto.response.ReportResponseDto;
import com.likelionknu.notdesign.report.data.entity.Report;
import com.likelionknu.notdesign.report.data.entity.ReportItems;
import com.likelionknu.notdesign.report.data.repository.ReportItemsRepository;
import com.likelionknu.notdesign.report.data.repository.ReportRepository;
import com.likelionknu.notdesign.report.exception.ReportAccessDeniedException;
import com.likelionknu.notdesign.report.exception.ReportNotFoundException;
import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.result.data.repository.ResultRepository;
import com.likelionknu.notdesign.result.exception.ResultNotFoundException;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ReportItemsRepository reportItemsRepository;
    private final ResultRepository resultRepository;
    private final UserRepository userRepository;
    private final PlanProcessRepository planProcessRepository;
    private final ReportAttributionEngine attributionEngine;

    private void checkOwner(Long userId, Report report) {
        if (!report.getResult().getUser().getId().equals(userId)) {
            throw new ReportAccessDeniedException(userId, report.getId());
        }
    }

    private Result getBaseline(Report report) {
        Long planId = report.getPlan().getId();
        PlanProcess process = planProcessRepository.findFirstByPlan_IdOrFuturePlan_Id(planId, planId)
                .orElseThrow(() -> new ResultNotFoundException(planId));

        return resultRepository
                .findFirstByUser_IdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
                        process.getUser().getId(), process.getStartedAt().atStartOfDay())
                .orElseThrow(() -> new ResultNotFoundException(planId));
    }

    private List<ReportMetricResponseDto> getMetrics(Report report) {
        Result before = getBaseline(report);
        Result after = report.getResult();

        List<ReportMetricResponseDto> metrics = new ArrayList<>();

        metrics.add(ReportMetricResponseDto.builder()
                .improvement(ImprovementItem.PIGMENTATION)
                .improvementName(ImprovementItem.PIGMENTATION.getDisplayName())
                .before(before.getPigmentation())
                .after(after.getPigmentation())
                .delta(report.getPigmentationDelta())
                .build());

        metrics.add(ReportMetricResponseDto.builder()
                .improvement(ImprovementItem.PORES)
                .improvementName(ImprovementItem.PORES.getDisplayName())
                .before(before.getPores())
                .after(after.getPores())
                .delta(report.getPoresDelta())
                .build());

        metrics.add(ReportMetricResponseDto.builder()
                .improvement(ImprovementItem.ERYTHEMA)
                .improvementName(ImprovementItem.ERYTHEMA.getDisplayName())
                .before(before.getErythema())
                .after(after.getErythema())
                .delta(report.getErythemaDelta())
                .build());

        return metrics;
    }

    private List<ReportContributionResponseDto> getContributions(Long reportId) {
        List<ReportItems> reportItemList =
                reportItemsRepository.findAllByReport_IdOrderByImprovementAscContributionRateDesc(reportId);

        List<ReportContributionResponseDto> contributions = new ArrayList<>();

        for (ReportItems reportItem : reportItemList) {
            PlanItem planItem = reportItem.getTimeline().getItem();

            contributions.add(ReportContributionResponseDto.builder()
                    .improvement(reportItem.getImprovement())
                    .improvementName(reportItem.getImprovement().getDisplayName())
                    .category(planItem.getCategory())
                    .categoryName(planItem.getCategory().getDisplayName())
                    .content(planItem.getName())
                    .score(reportItem.getScore())
                    .contributionRate(reportItem.getContributionRate())
                    .price(reportItem.getPrice())
                    .costPerPoint(reportItem.getCostPerPoint())
                    .reliability(reportItem.getReliability())
                    .build());
        }

        return contributions;
    }

    private List<ReportExecutionResponseDto> getExecutions(Report report) {
        Long planId = report.getPlan().getId();

        PlanProcess process = planProcessRepository.findFirstByPlan_IdOrFuturePlan_Id(planId, planId)
                .orElse(null);

        if (process == null) {
            return List.of();
        }

        List<ReportExecutionResponseDto> executions = new ArrayList<>();

        for (ReportAttributionEngine.WeeklyExecution execution : attributionEngine.getWeeklyExecutions(
                process.getId(), planId, process.getStartedAt(),
                report.getPlan().getDurationWeeks(), report.getResult().getMeasuredAt().toLocalDate())) {
            PlanItem item = execution.timeline().getItem();

            executions.add(ReportExecutionResponseDto.builder()
                    .category(item.getCategory())
                    .categoryName(item.getCategory().getDisplayName())
                    .content(item.getName())
                    .plannedWeeks(execution.plannedWeeks())
                    .doneWeeks(execution.doneWeeks())
                    .build());
        }

        return executions;
    }

    private ReportResponseDto toResponse(Report report) {
        return ReportResponseDto.builder()
                .reportId(report.getId())
                .type(report.getType())
                .summary(report.getSummary())
                .nextPlanSuggestion(report.getNextPlanSuggestion())
                .nextPlanPrice(report.getNextPlanPrice())
                .metrics(getMetrics(report))
                .contributions(getContributions(report.getId()))
                .executions(getExecutions(report))
                .build();
    }

    /**
     * 리포트 id 로 기여도 리포트를 조회합니다.
     *
     * @param email 조회 대상이 되는 사용자
     * @param reportId 조회할 리포트
     * @return 지표 변화, 항목별 기여도, 1점당 비용
     */
    @Transactional(readOnly = true)
    public ReportResponseDto getReport(String email, Long reportId) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        Report report = reportRepository.findWithPlanAndResultById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        checkOwner(user.getId(), report);

        return toResponse(report);
    }

    /**
     * 가장 최근에 받은 기여도 리포트를 조회합니다.
     *
     * @param email 조회 대상이 되는 사용자
     * @return 지표 변화, 항목별 기여도, 1점당 비용
     */
    @Transactional(readOnly = true)
    public ReportResponseDto getLatestReport(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        Report report = reportRepository.findFirstByResult_User_IdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(ReportNotFoundException::new);

        return toResponse(report);
    }
}
