package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.diary.data.entity.DailyChecklist;
import com.likelionknu.notdesign.diary.data.repository.DailyChecklistRepository;
import com.likelionknu.notdesign.diary.data.repository.DiaryRepository;
import com.likelionknu.notdesign.plan.data.dto.response.PlanDetailItemResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanDetailResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanStatsResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanStartResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanSummaryResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanTodoResponseDto;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import com.likelionknu.notdesign.plan.data.entity.PlanTimelineWeek;
import com.likelionknu.notdesign.plan.data.repository.PlanProcessRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineWeekRepository;
import com.likelionknu.notdesign.plan.exception.PlanAlreadyInProgressException;
import com.likelionknu.notdesign.plan.exception.PlanAlreadyStartedException;
import com.likelionknu.notdesign.plan.exception.PlanCycleNotFinishedException;
import com.likelionknu.notdesign.plan.exception.PlanNotFoundException;
import com.likelionknu.notdesign.plan.exception.PlanProcessNotFoundException;
import com.likelionknu.notdesign.result.data.repository.ResultRepository;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DAYS_PER_WEEK = 7;
    private static final int MID_REPORT_WEEK = 6;

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanProcessRepository planProcessRepository;
    private final PlanTimelineRepository planTimelineRepository;
    private final PlanTimelineWeekRepository planTimelineWeekRepository;
    private final ResultRepository resultRepository;
    private final DiaryRepository diaryRepository;
    private final DailyChecklistRepository dailyChecklistRepository;

    /**
     * 홈 화면에서 현재 진행 중인 플랜의 진행 요약을 조회합니다.
     *
     * @param email 조회 대상 사용자
     * @return 진행 주차, 진행률, 중간/최종 리포트까지 남은 기간
     */
    @Transactional(readOnly = true)
    public PlanSummaryResponseDto getCurrentPlanSummary(String email) {
        PlanProcess process = getCurrentProcess(email);
        Progress progress = computeProgress(process);

        LocalDate today = LocalDate.now(KST);
        LocalDate startedAt = process.getStartedAt();

        // 중간=시작+6주, 최종=시작+12주 시점 (report/result 측정일 더미데이터로 검증)
        int daysToMidReport = daysUntil(today, startedAt.plusWeeks(MID_REPORT_WEEK));
        int daysToFinalReport = daysUntil(today, startedAt.plusWeeks(progress.totalWeeks()));

        return PlanSummaryResponseDto.builder()
                .currentWeek(progress.currentWeek())
                .totalWeeks(progress.totalWeeks())
                .progressRate(progress.progressRate())
                .daysToMidReport(daysToMidReport)
                .daysToFinalReport(daysToFinalReport)
                .build();
    }

    /**
     * 플랜 상세 화면에서 현재 진행 중인 플랜의 진단 지표, 예상 비용, 관리 항목을 조회합니다.
     *
     * @param email 조회 대상 사용자
     * @return 최근 측정 지표, 총 예상 비용, 카테고리별 관리 항목(주차·이유 포함)
     */
    @Transactional(readOnly = true)
    public PlanDetailResponseDto getCurrentPlanDetail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        PlanProcess process = planProcessRepository.findByUser_IdAndCompletedAtIsNull(user.getId())
                .orElseThrow(() -> new PlanProcessNotFoundException(user.getId()));

        Plan plan = process.getPlan();

        // 최근 측정 결과 (측정 이력이 없으면 지표 없음)
        PlanDetailResponseDto.Metrics metrics = resultRepository.findAllByUserOrderByMeasuredAtDesc(user)
                .stream().findFirst()
                .map(result -> PlanDetailResponseDto.Metrics.builder()
                        .pigmentation(result.getPigmentation())
                        .hydration(result.getHydration())
                        .erythema(result.getErythema())
                        .build())
                .orElse(null);

        // 플랜에 속한 관리 항목(타임라인)과 각 항목의 진행 주차
        List<PlanTimeline> timelines = planTimelineRepository.findAllByPlan_IdOrderByItem_IdAsc(plan.getId());
        Map<Long, List<Integer>> weeksByTimeline = groupWeeksByTimeline(timelines);

        List<PlanDetailItemResponseDto> items = timelines.stream()
                .map(timeline -> toItem(timeline, weeksByTimeline))
                .toList();

        return PlanDetailResponseDto.builder()
                .planSummary(plan.getSummary())
                .totalPrice(plan.getTotalPrice())
                .metrics(metrics)
                .items(items)
                .build();
    }

    /**
     * 마이 화면에서 현재 진행 중인 플랜의 기록 통계를 조회합니다.
     *
     * @param email 조회 대상 사용자
     * @return 진행 주차·진행률·경과일과 남긴 하루 기록 수
     */
    @Transactional(readOnly = true)
    public PlanStatsResponseDto getCurrentPlanStats(String email) {
        PlanProcess process = getCurrentProcess(email);
        Progress progress = computeProgress(process);

        int recordedDays = (int) diaryRepository.countByProcess_Id(process.getId());

        return PlanStatsResponseDto.builder()
                .currentWeek(progress.currentWeek())
                .totalWeeks(progress.totalWeeks())
                .progressRate(progress.progressRate())
                .elapsedDays(progress.elapsedDays())
                .recordedDays(recordedDays)
                .build();
    }

    /**
     * 하루 기록 생성 화면에서 오늘 실천할 플랜 항목(체크리스트)을 조회합니다.
     *
     * @param email 조회 대상 사용자
     * @return 오늘 날짜의 실천 항목 목록 (기록 생성 전이라 실천 여부는 모두 미완료)
     */
    @Transactional(readOnly = true)
    public List<PlanTodoResponseDto> getCurrentPlanTodos(String email) {
        PlanProcess process = getCurrentProcess(email);
        LocalDate today = LocalDate.now(KST);

        return dailyChecklistRepository
                .findAllByProcess_IdAndTargetDateOrderByTimeline_Item_IdAsc(process.getId(), today)
                .stream()
                .map(this::toTodo)
                .toList();
    }

    /**
     * 생성된 플랜을 현재 사용자의 진행 중 플랜으로 시작합니다.
     *
     * @param email  시작을 요청한 사용자
     * @param planId 시작할 플랜 ID
     * @return 생성된 진행(PlanProcess) ID와 시작일
     * @throws PlanAlreadyInProgressException 이미 진행 중인 플랜이 있는 경우
     * @throws PlanNotFoundException          플랜을 찾을 수 없는 경우
     */
    @Transactional
    public PlanStartResponseDto startPlan(String email, Long planId) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        // 진행 중 플랜은 사용자당 하나만 허용 (현재 플랜 조회 API가 단일 조회 전제)
        if (planProcessRepository.existsByUser_IdAndCompletedAtIsNull(user.getId())) {
            throw new PlanAlreadyInProgressException(user.getId());
        }

        Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        PlanProcess process = PlanProcess.builder()
                .plan(plan)
                .user(user)
                .startedAt(LocalDate.now(KST))
                .build();
        planProcessRepository.save(process);

        return PlanStartResponseDto.builder()
                .processId(process.getId())
                .startedAt(process.getStartedAt())
                .build();
    }

    /**
     * 12주를 마친 사이클을 종료하고, 다음 플랜으로 새 사이클을 시작합니다.
     *
     * @param email 시작을 요청한 사용자
     * @param planId 다음 사이클에서 사용할 플랜 ID
     * @return 새로 생성된 진행(PlanProcess) ID와 시작일
     * @throws PlanProcessNotFoundException 진행 중인 사이클이 없는 경우
     * @throws PlanCycleNotFinishedException 진행 중인 사이클이 아직 12주를 채우지 못한 경우
     * @throws PlanNotFoundException 플랜을 찾을 수 없는 경우
     */
    @Transactional
    public PlanStartResponseDto startNextPlan(String email, Long planId) {
        PlanProcess process = getCurrentProcess(email);
        Progress progress = computeProgress(process);

        if (progress.elapsedDays() < progress.totalWeeks() * DAYS_PER_WEEK) {
            throw new PlanCycleNotFinishedException(process.getId(), progress.currentWeek(), progress.totalWeeks());
        }

        Plan nextPlan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        LocalDate today = LocalDate.now(KST);
        process.complete(nextPlan, today);

        PlanProcess nextProcess = PlanProcess.builder()
                .plan(nextPlan)
                .user(process.getUser())
                .startedAt(today)
                .build();
        planProcessRepository.save(nextProcess);

        return PlanStartResponseDto.builder()
                .processId(nextProcess.getId())
                .startedAt(nextProcess.getStartedAt())
                .build();
    }

    /**
     * 아직 시작하지 않은 플랜을 삭제합니다. (플랜 생성 후 뒤로가기)
     *
     * @param planId 삭제할 플랜 ID
     * @throws PlanNotFoundException       플랜을 찾을 수 없는 경우
     * @throws PlanAlreadyStartedException 이미 시작(진행)에 연결된 플랜인 경우
     */
    @Transactional
    public void deletePlan(Long planId) {
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        // 이미 진행(PlanProcess)에 연결된 플랜은 삭제 불가
        if (planProcessRepository.existsByPlan_Id(planId)) {
            throw new PlanAlreadyStartedException(planId);
        }

        // 자식(주차 → 타임라인)부터 삭제 후 플랜 삭제 (plan_items 공유 카탈로그는 보존)
        List<Long> timelineIds = planTimelineRepository.findAllByPlan_IdOrderByItem_IdAsc(planId)
                .stream().map(PlanTimeline::getId).toList();
        if (!timelineIds.isEmpty()) {
            planTimelineWeekRepository.deleteByTimeline_IdIn(timelineIds);
        }
        planTimelineRepository.deleteByPlan_Id(planId);
        planRepository.delete(plan);
    }

    private PlanProcess getCurrentProcess(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        return planProcessRepository.findByUser_IdAndCompletedAtIsNull(user.getId())
                .orElseThrow(() -> new PlanProcessNotFoundException(user.getId()));
    }

    private Progress computeProgress(PlanProcess process) {
        int totalWeeks = process.getPlan().getDurationWeeks();
        long totalDays = (long) totalWeeks * DAYS_PER_WEEK;

        LocalDate today = LocalDate.now(KST);
        // 시작 전이면 0일, 전체 기간을 넘겼으면 전체 일수로 고정
        long elapsedDays = Math.max(0, Math.min(ChronoUnit.DAYS.between(process.getStartedAt(), today), totalDays));

        int currentWeek = Math.min((int) (elapsedDays / DAYS_PER_WEEK) + 1, totalWeeks);
        int progressRate = (int) Math.round(elapsedDays * 100.0 / totalDays);

        return new Progress(currentWeek, totalWeeks, progressRate, (int) elapsedDays);
    }

    private Map<Long, List<Integer>> groupWeeksByTimeline(List<PlanTimeline> timelines) {
        List<Long> timelineIds = timelines.stream().map(PlanTimeline::getId).toList();

        return planTimelineWeekRepository.findAllByTimeline_IdInOrderByWeekAsc(timelineIds).stream()
                .collect(Collectors.groupingBy(
                        week -> week.getTimeline().getId(),
                        Collectors.mapping(PlanTimelineWeek::getWeek, Collectors.toList())));
    }

    private PlanDetailItemResponseDto toItem(PlanTimeline timeline, Map<Long, List<Integer>> weeksByTimeline) {
        PlanItem item = timeline.getItem();

        return PlanDetailItemResponseDto.builder()
                .category(item.getCategory())
                .categoryName(item.getCategory().getDisplayName())
                .name(item.getName())
                .frequency(item.getFrequency())
                .price(item.getPrice())
                .weeks(weeksByTimeline.getOrDefault(timeline.getId(), List.of()))
                .reason(timeline.getReason())
                .build();
    }

    private PlanTodoResponseDto toTodo(DailyChecklist checklist) {
        PlanItem item = checklist.getTimeline().getItem();

        return PlanTodoResponseDto.builder()
                .checklistId(checklist.getId())
                .category(item.getCategory())
                .categoryName(item.getCategory().getDisplayName())
                .name(item.getName())
                .done(false)
                .build();
    }

    private int daysUntil(LocalDate today, LocalDate target) {
        return (int) Math.max(0, ChronoUnit.DAYS.between(today, target));
    }

    private record Progress(int currentWeek, int totalWeeks, int progressRate, int elapsedDays) {
    }
}
