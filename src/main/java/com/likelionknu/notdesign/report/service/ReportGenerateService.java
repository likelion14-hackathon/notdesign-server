package com.likelionknu.notdesign.report.service;

import com.likelionknu.notdesign.diary.data.entity.Diary;
import com.likelionknu.notdesign.diary.data.repository.DiaryRepository;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.plan.data.repository.PlanProcessRepository;
import com.likelionknu.notdesign.plan.exception.PlanProcessNotFoundException;
import com.likelionknu.notdesign.report.data.dto.response.ReportResponseDto;
import com.likelionknu.notdesign.report.data.entity.Report;
import com.likelionknu.notdesign.report.data.entity.ReportItems;
import com.likelionknu.notdesign.report.data.enums.ReportType;
import com.likelionknu.notdesign.report.data.repository.ReportItemsRepository;
import com.likelionknu.notdesign.report.data.repository.ReportRepository;
import com.likelionknu.notdesign.report.exception.ResultNotImportedException;
import com.likelionknu.notdesign.report.service.ReportAttributionEngine.Attribution;
import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.result.data.repository.ResultRepository;
import com.likelionknu.notdesign.result.exception.ResultNotFoundException;
import com.likelionknu.notdesign.result.service.ResultService;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGenerateService {
    private static final int DAYS_PER_WEEK = 7;
    private static final BigDecimal MEANINGFUL_RATE = BigDecimal.valueOf(10);

    private final UserRepository userRepository;
    private final PlanProcessRepository planProcessRepository;
    private final ResultRepository resultRepository;
    private final ReportRepository reportRepository;
    private final ReportItemsRepository reportItemsRepository;
    private final DiaryRepository diaryRepository;
    private final ReportAttributionEngine attributionEngine;
    private final ReportAiClient reportAiClient;
    private final ReportService reportService;
    private final ResultService resultService;

    /**
     * 불러온 측정 데이터로 기여도 리포트를 생성합니다.
     *
     * @param email 리포트를 생성할 사용자
     * @return 생성된 리포트
     * @throws PlanProcessNotFoundException 진행 중인 사이클이 없는 경우
     * @throws ResultNotImportedException   리포트를 만들 새 측정 데이터가 없는 경우
     * @throws ResultNotFoundException      사이클의 기준선 측정이 없는 경우
     */
    @Transactional
    public ReportResponseDto generateReport(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        PlanProcess process = planProcessRepository.findByUser_IdAndCompletedAtIsNull(user.getId())
                .orElseThrow(() -> new PlanProcessNotFoundException(user.getId()));

        Plan plan = process.getActivePlan();
        Result measured = getMeasured(user);

        LocalDate measuredDate = measured.getMeasuredAt().toLocalDate();
        int elapsedWeeks = countElapsedWeeks(process.getStartedAt(), measuredDate);

        Result baseline = getBaseline(user, process);

        if (baseline.getId().equals(measured.getId())) {
            throw new ResultNotImportedException(user.getId());
        }

        Map<ImprovementItem, Integer> deltas = computeDeltas(baseline, measured);
        ReportType type = decideType(process, plan, elapsedWeeks);

        List<Attribution> attributions =
                attributionEngine.attribute(process.getId(), plan.getId(), elapsedWeeks, measuredDate, deltas);

        ReportAiClient.Sentences sentences = reportAiClient.write(
                type, describe(process, plan, baseline, measured, deltas, attributions, measuredDate));

        Report report = save(plan, measured, type, deltas, sentences, attributions);

        return reportService.getReport(email, report.getId());
    }

    private Result getMeasured(User user) {
        Result measured = resultRepository.findFirstByUser_IdOrderByMeasuredAtDesc(user.getId())
                .orElseGet(() -> importDummy(user, null));

        if (reportRepository.existsByResult_Id(measured.getId())) {
            return importDummy(user, measured.getMeasuredAt());
        }

        return measured;
    }

    private Result importDummy(User user, LocalDateTime after) {
        try {
            return resultService.importDummy(user, null, after);
        } catch (com.likelionknu.notdesign.result.data.exception.ResultNotFoundException e) {
            throw new ResultNotImportedException(user.getId());
        }
    }

    private Result getBaseline(User user, PlanProcess process) {
        return resultRepository
                .findFirstByUser_IdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
                        user.getId(), process.getStartedAt().atStartOfDay())
                .orElseThrow(() -> new ResultNotFoundException(process.getPlan().getId()));
    }

    private int countElapsedWeeks(LocalDate startedAt, LocalDate measuredDate) {
        long days = Math.max(0, ChronoUnit.DAYS.between(startedAt, measuredDate));

        return (int) (days / DAYS_PER_WEEK);
    }

    private Map<ImprovementItem, Integer> computeDeltas(Result baseline, Result measured) {
        Map<ImprovementItem, Integer> deltas = new EnumMap<>(ImprovementItem.class);

        deltas.put(ImprovementItem.PIGMENTATION, measured.getPigmentation() - baseline.getPigmentation());
        deltas.put(ImprovementItem.PORES, measured.getPores() - baseline.getPores());
        deltas.put(ImprovementItem.ERYTHEMA, measured.getErythema() - baseline.getErythema());

        return deltas;
    }

    private ReportType decideType(PlanProcess process, Plan plan, int elapsedWeeks) {
        List<Long> planIds = new ArrayList<>();

        planIds.add(process.getPlan().getId());

        if (process.getFuturePlan() != null) {
            planIds.add(process.getFuturePlan().getId());
        }

        if (reportRepository.existsByPlan_IdInAndType(planIds, ReportType.MID)) {
            return ReportType.FINAL;
        }

        return elapsedWeeks >= plan.getDurationWeeks() ? ReportType.FINAL : ReportType.MID;
    }

    private Report save(Plan plan, Result measured, ReportType type, Map<ImprovementItem, Integer> deltas,
                        ReportAiClient.Sentences sentences, List<Attribution> attributions) {
        boolean isFinal = type == ReportType.FINAL;

        Report report = reportRepository.save(Report.builder()
                .type(type)
                .plan(plan)
                .result(measured)
                .summary(sentences.summary())
                .nextPlanSuggestion(sentences.nextPlanSuggestion())
                .nextPlanPrice(isFinal ? computeNextPlanPrice(plan, attributions) : null)
                .pigmentationDelta(deltas.get(ImprovementItem.PIGMENTATION))
                .poresDelta(deltas.get(ImprovementItem.PORES))
                .erythemaDelta(deltas.get(ImprovementItem.ERYTHEMA))
                .build());

        for (Attribution attribution : attributions) {
            reportItemsRepository.save(ReportItems.builder()
                    .report(report)
                    .improvement(attribution.improvement())
                    .timeline(attribution.timeline())
                    .score(attribution.score())
                    .contributionRate(attribution.contributionRate())
                    .costPerPoint(attribution.costPerPoint())
                    .price(attribution.price())
                    .reliability(attribution.reliability())
                    .build());
        }

        return report;
    }

    private int computeNextPlanPrice(Plan plan, List<Attribution> attributions) {
        Map<Long, Integer> droppedPrices = new LinkedHashMap<>();

        for (Attribution attribution : attributions) {
            Long timelineId = attribution.timeline().getId();

            if (attribution.contributionRate().compareTo(MEANINGFUL_RATE) >= 0) {
                droppedPrices.put(timelineId, 0);
            } else {
                droppedPrices.putIfAbsent(timelineId, attribution.timeline().getItem().getPrice());
            }
        }

        int dropped = 0;

        for (Integer price : droppedPrices.values()) {
            dropped += price;
        }

        return Math.max(0, plan.getTotalPrice() - dropped);
    }

    private String describe(PlanProcess process, Plan plan, Result baseline, Result measured,
                            Map<ImprovementItem, Integer> deltas, List<Attribution> attributions,
                            LocalDate measuredDate) {
        StringBuilder text = new StringBuilder();

        text.append("# 플랜\n").append(plan.getSummary())
                .append("\n시작일 ").append(process.getStartedAt())
                .append(", 측정일 ").append(measuredDate)
                .append("\n\n# 지표 변화 (기준선 → 측정값, 변화량)\n");

        text.append(describeMetric("색소침착", baseline.getPigmentation(), measured.getPigmentation(),
                deltas.get(ImprovementItem.PIGMENTATION)));
        text.append(describeMetric("모공", baseline.getPores(), measured.getPores(),
                deltas.get(ImprovementItem.PORES)));
        text.append(describeMetric("홍조", baseline.getErythema(), measured.getErythema(),
                deltas.get(ImprovementItem.ERYTHEMA)));

        text.append("\n# 항목별 기여도 (서버 계산 완료)\n");

        for (ImprovementItem improvement : ImprovementItem.activeValues()) {
            text.append("- ").append(improvement.getDisplayName()).append("\n");

            for (Attribution attribution : attributions) {
                if (attribution.improvement() == improvement) {
                    text.append(describeAttribution(attribution));
                }
            }
        }

        text.append("\n# 일기 (슬라이더 0~10)\n").append(describeDiaries(process.getId(), measured));

        return text.toString();
    }

    private String describeMetric(String name, int before, int after, int delta) {
        return "- %s %d → %d (%+d점)%n".formatted(name, before, after, delta);
    }

    private String describeAttribution(Attribution attribution) {
        PlanItem item = attribution.timeline().getItem();

        if (attribution.score().signum() == 0) {
            return "  · %s (%s) 기여 신호 없음%n".formatted(item.getName(), item.getCategory().getDisplayName());
        }

        String costPerPoint = attribution.costPerPoint() == null
                ? "계산 불가"
                : "%d원/점".formatted(attribution.costPerPoint());

        return "  · %s (%s) %s점, 기여율 %s%%, 비용 %d원, %s, 신뢰도 %s%n".formatted(
                item.getName(), item.getCategory().getDisplayName(),
                attribution.score().stripTrailingZeros().toPlainString(),
                attribution.contributionRate().stripTrailingZeros().toPlainString(),
                attribution.price(), costPerPoint, attribution.reliability().name());
    }

    private String describeDiaries(Long processId, Result measured) {
        List<Diary> diaries = diaryRepository
                .findAllByProcess_IdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(processId, measured.getMeasuredAt());

        if (diaries.isEmpty()) {
            return "(기록 없음)\n";
        }

        StringBuilder text = new StringBuilder();

        for (Diary diary : diaries) {
            if (diary.getComment() == null || diary.getComment().isBlank()) {
                continue;
            }

            text.append("- ").append(diary.getRecordedAt().toLocalDate())
                    .append(" 피부톤 ").append(diary.getSkinTone())
                    .append(", 모공 ").append(diary.getPores())
                    .append(", 홍조 ").append(diary.getFlushing())
                    .append(" · ").append(diary.getComment()).append("\n");
        }

        return text.isEmpty() ? "(기록 없음)\n" : text.toString();
    }
}
