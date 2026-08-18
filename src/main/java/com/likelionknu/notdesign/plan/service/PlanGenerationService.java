package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse;
import com.likelionknu.notdesign.plan.data.dto.request.PlanCreateRequestDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanCreateResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanDetailItemResponseDto;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import com.likelionknu.notdesign.plan.data.entity.PlanTimelineWeek;
import com.likelionknu.notdesign.plan.data.enums.PlanGenerationMode;
import com.likelionknu.notdesign.plan.data.repository.PlanProcessRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineWeekRepository;
import com.likelionknu.notdesign.plan.exception.PlanGenerationFailedException;
import com.likelionknu.notdesign.plan.exception.PlanProcessNotFoundException;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.Catalog;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.CatalogEntry;
import com.likelionknu.notdesign.report.data.entity.Report;
import com.likelionknu.notdesign.report.data.entity.ReportItems;
import com.likelionknu.notdesign.report.data.enums.ReportType;
import com.likelionknu.notdesign.report.data.repository.ReportItemsRepository;
import com.likelionknu.notdesign.report.data.repository.ReportRepository;
import com.likelionknu.notdesign.report.exception.ReportNotFoundException;
import com.likelionknu.notdesign.report.service.ReportAttributionEngine;
import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.result.data.exception.ResultNotFoundException;
import com.likelionknu.notdesign.result.data.repository.ResultRepository;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationService {
    private final UserRepository userRepository;
    private final ResultRepository resultRepository;
    private final ReportRepository reportRepository;
    private final ReportItemsRepository reportItemsRepository;
    private final ReportAttributionEngine attributionEngine;
    private final PlanProcessRepository planProcessRepository;
    private final PlanTimelineRepository planTimelineRepository;
    private final PlanTimelineWeekRepository planTimelineWeekRepository;
    private final PlanCatalogService planCatalogService;
    private final PlanPromptBuilder planPromptBuilder;
    private final PlanAiClient planAiClient;
    private final PlanGenerationValidator planGenerationValidator;
    private final PlanAssembler planAssembler;

    /**
     * 플랜을 생성한다.
     *
     * @param email   요청 사용자
     * @param request 생성 모드와 모드별 입력
     * @return 생성된 플랜 미리보기(planId 포함)
     */
    public PlanCreateResponseDto generate(String email, PlanCreateRequestDto request) {
        PlanGenerationMode mode = request.getMode();

        Catalog catalog = planCatalogService.load();
        String systemPrompt = mode == PlanGenerationMode.TRIAL
                ? planPromptBuilder.buildSystemTrial(catalog)
                : planPromptBuilder.buildSystem(catalog);
        String userPrompt = buildUserPrompt(email, request, mode, catalog);

        PlanGenerationAiResponse aiResponse = generateWithRetry(systemPrompt, userPrompt, catalog, mode.getDurationWeeks());
        Plan plan = planAssembler.assemble(mode, aiResponse, catalog);

        return toResponse(plan, mode, aiResponse, catalog);
    }

    private String buildUserPrompt(String email, PlanCreateRequestDto request, PlanGenerationMode mode, Catalog catalog) {
        return switch (mode) {
            case NEW -> {
                User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
                Result result = resultRepository.findAllByUserOrderByMeasuredAtDesc(user).stream()
                        .findFirst()
                        .orElseThrow(ResultNotFoundException::new);
                yield planPromptBuilder.buildUserNew(
                        result.getPigmentation(), result.getPores(), result.getErythema(), request.getMonthlyBudget());
            }
            case NEXT -> buildNextPrompt(email, catalog);
            case ADJUST -> buildAdjustPrompt(email, catalog);
            case TRIAL -> buildTrialPrompt(request);
        };
    }

    private String buildNextPrompt(String email, Catalog catalog) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
        Report report = reportRepository
                .findFirstByResult_User_IdAndTypeOrderByCreatedAtDesc(user.getId(), ReportType.FINAL)
                .orElseThrow(ReportNotFoundException::new);

        return planPromptBuilder.buildUserNext(
                toMetricChanges(report), toPreviousItems(report, catalog),
                report.getPlan().getTotalPrice(), report.getNextPlanPrice());
    }

    private List<PlanPromptBuilder.MetricChange> toMetricChanges(Report report) {
        Result measured = report.getResult();
        return List.of(
                toMetricChange("색소침착", measured.getPigmentation(), report.getPigmentationDelta()),
                toMetricChange("모공", measured.getPores(), report.getPoresDelta()),
                toMetricChange("홍조", measured.getErythema(), report.getErythemaDelta()));
    }

    private PlanPromptBuilder.MetricChange toMetricChange(String name, int measured, int delta) {
        return new PlanPromptBuilder.MetricChange(name, measured - delta, measured, delta);
    }

    private List<PlanPromptBuilder.PreviousItem> toPreviousItems(Report report, Catalog catalog) {
        List<PlanTimeline> timelines =
                planTimelineRepository.findAllByPlan_IdOrderByItem_IdAsc(report.getPlan().getId());
        List<Long> timelineIds = timelines.stream().map(PlanTimeline::getId).toList();

        Map<Long, List<Integer>> weeksByTimeline = planTimelineWeekRepository
                .findAllByTimeline_IdInOrderByWeekAsc(timelineIds).stream()
                .collect(Collectors.groupingBy(week -> week.getTimeline().getId(),
                        Collectors.mapping(PlanTimelineWeek::getWeek, Collectors.toList())));

        Map<Long, List<ReportItems>> attributionsByTimeline = reportItemsRepository
                .findAllByReport_IdOrderByImprovementAscContributionRateDesc(report.getId()).stream()
                .collect(Collectors.groupingBy(reportItem -> reportItem.getTimeline().getId()));

        List<PlanPromptBuilder.PreviousItem> previousItems = new ArrayList<>();
        for (PlanTimeline timeline : timelines) {
            PlanItem item = timeline.getItem();
            CatalogEntry entry = catalog.resolveByName(item.getName());
            if (entry == null) {
                throw new PlanGenerationFailedException("카탈로그에서 항목을 찾을 수 없습니다: " + item.getName());
            }
            previousItems.add(new PlanPromptBuilder.PreviousItem(
                    entry.id(), item.getName(), item.getCategory().getDisplayName(),
                    weeksByTimeline.getOrDefault(timeline.getId(), List.of()),
                    item.getFrequency(),
                    toAttributionLines(attributionsByTimeline.getOrDefault(timeline.getId(), List.of()))));
        }
        return previousItems;
    }

    private List<PlanPromptBuilder.AttributionLine> toAttributionLines(List<ReportItems> reportItems) {
        return reportItems.stream()
                .map(reportItem -> new PlanPromptBuilder.AttributionLine(
                        reportItem.getImprovement().getDisplayName(),
                        reportItem.getScore(), reportItem.getContributionRate(),
                        reportItem.getReliability() == null ? "" : reportItem.getReliability().name()))
                .toList();
    }

    private String buildAdjustPrompt(String email, Catalog catalog) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
        PlanProcess process = planProcessRepository.findByUser_IdAndCompletedAtIsNull(user.getId())
                .orElseThrow(() -> new PlanProcessNotFoundException(user.getId()));

        List<Long> planIds = new ArrayList<>();
        planIds.add(process.getPlan().getId());
        if (process.getFuturePlan() != null) {
            planIds.add(process.getFuturePlan().getId());
        }

        Report report = reportRepository
                .findFirstByPlan_IdInAndTypeOrderByCreatedAtDesc(planIds, ReportType.MID)
                .orElseThrow(ReportNotFoundException::new);

        Plan currentPlan = report.getPlan();
        LocalDate measuredDate = report.getResult().getMeasuredAt().toLocalDate();
        int elapsedWeeks = (int) ChronoUnit.WEEKS.between(process.getStartedAt(), measuredDate);

        List<PlanPromptBuilder.ExecutionLine> executions = toExecutionLines(
                attributionEngine.getWeeklyExecutions(process.getId(), currentPlan.getId(),
                        process.getStartedAt(), currentPlan.getDurationWeeks(), measuredDate));

        return planPromptBuilder.buildUserAdjust(
                toMetricChanges(report), elapsedWeeks, toPreviousItems(report, catalog), executions);
    }

    private List<PlanPromptBuilder.ExecutionLine> toExecutionLines(
            List<ReportAttributionEngine.WeeklyExecution> executions) {
        return executions.stream()
                .map(execution -> new PlanPromptBuilder.ExecutionLine(
                        execution.timeline().getItem().getName(),
                        execution.plannedWeeks(), execution.doneWeeks()))
                .toList();
    }

    private String buildTrialPrompt(PlanCreateRequestDto request) {
        Double skinTone = request.getSkinTone();
        Double pores = request.getPores();
        Double redness = request.getRedness();
        if (skinTone == null || pores == null || redness == null) {
            throw new PlanGenerationFailedException("TRIAL 모드는 skinTone·pores·redness 가 모두 필요합니다.");
        }

        return planPromptBuilder.buildUserTrial(skinTone, pores, redness);
    }

    private PlanGenerationAiResponse generateWithRetry(String systemPrompt, String userPrompt,
                                                       Catalog catalog, int durationWeeks) {
        PlanGenerationAiResponse response = planAiClient.generate(systemPrompt, userPrompt);
        List<String> violations = planGenerationValidator.validate(response, catalog, durationWeeks);
        if (violations.isEmpty()) {
            return response;
        }

        log.warn("[PlanGenerationService] AI 응답 규칙 위반, 1회 재요청: {}", violations);
        String retryPrompt = userPrompt + "\n\n# 이전 응답의 오류 (반드시 고쳐서 다시 생성)\n- "
                + String.join("\n- ", violations);

        PlanGenerationAiResponse retried = planAiClient.generate(systemPrompt, retryPrompt);
        List<String> retryViolations = planGenerationValidator.validate(retried, catalog, durationWeeks);
        if (!retryViolations.isEmpty()) {
            throw new PlanGenerationFailedException("재요청 후에도 규칙 위반: " + retryViolations);
        }

        return retried;
    }

    private PlanCreateResponseDto toResponse(Plan plan, PlanGenerationMode mode,
                                             PlanGenerationAiResponse aiResponse, Catalog catalog) {
        List<PlanDetailItemResponseDto> items = aiResponse.items().stream()
                .map(item -> {
                    CatalogEntry entry = catalog.resolve(item.itemEffectId().longValue());
                    return PlanDetailItemResponseDto.builder()
                            .category(entry.category())
                            .categoryName(entry.category().getDisplayName())
                            .content(entry.name())
                            .frequency(item.frequency())
                            .price(entry.price())
                            .weeks(item.weeks())
                            .reason(item.reason())
                            .build();
                })
                .toList();

        return PlanCreateResponseDto.builder()
                .planId(plan.getId())
                .mode(mode)
                .planSummary(plan.getSummary())
                .durationWeeks(plan.getDurationWeeks())
                .totalPrice(plan.getTotalPrice())
                .items(items)
                .build();
    }
}
