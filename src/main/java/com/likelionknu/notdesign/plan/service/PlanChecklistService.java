package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.diary.data.entity.DailyChecklist;
import com.likelionknu.notdesign.diary.data.repository.DailyChecklistRepository;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import com.likelionknu.notdesign.plan.data.entity.PlanTimelineWeek;
import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineWeekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanChecklistService {
    private static final int DAYS_PER_WEEK = 7;

    private final PlanTimelineRepository planTimelineRepository;
    private final PlanTimelineWeekRepository planTimelineWeekRepository;
    private final DailyChecklistRepository dailyChecklistRepository;

    /**
     * 플랜의 주차 구성을 날짜별 실천 항목으로 펼쳐 저장합니다.
     * 시술은 클리닉에서 받는 것이라 체크리스트에서 제외합니다.
     *
     * @param process 대상 사이클
     * @param plan    체크리스트를 펼칠 플랜
     * @param from    이 날짜부터 생성 (이전 날짜는 건너뜀)
     * @return 생성된 체크리스트 수
     */
    @Transactional
    public int generate(PlanProcess process, Plan plan, LocalDate from) {
        List<PlanTimeline> timelines = planTimelineRepository.findAllByPlan_IdOrderByItem_IdAsc(plan.getId());

        if (timelines.isEmpty()) {
            return 0;
        }

        Map<Long, List<Integer>> weeksByTimeline = groupWeeks(timelines);
        List<DailyChecklist> checklists = new ArrayList<>();

        for (PlanTimeline timeline : timelines) {
            if (timeline.getItem().getCategory() == PlanCategory.PROCEDURE) {
                continue;
            }

            for (Integer week : weeksByTimeline.getOrDefault(timeline.getId(), List.of())) {
                addWeek(checklists, process, timeline, week, from);
            }
        }

        dailyChecklistRepository.saveAll(checklists);
        log.info("[generate] 체크리스트 생성: processId={}, planId={}, {}건",
                process.getId(), plan.getId(), checklists.size());

        return checklists.size();
    }

    /**
     * 체크리스트가 한 건도 없는 사이클이면 활성 플랜 기준으로 전체 기간을 채웁니다.
     * 체크리스트 생성 코드가 없던 시점에 시작된 사이클을 위한 보정입니다.
     *
     * @param process 대상 사이클
     */
    @Transactional
    public void ensureGenerated(PlanProcess process) {
        if (dailyChecklistRepository.existsByProcess_Id(process.getId())) {
            return;
        }

        generate(process, process.getActivePlan(), process.getStartedAt());
    }

    /**
     * 조정 플랜을 적용할 때 아직 지나지 않은 날짜의 체크리스트를 지우고 새 플랜으로 다시 채웁니다.
     * 오늘 몫은 이미 기록에 묶여 있을 수 있어 남겨 둡니다.
     *
     * @param process 대상 사이클
     * @param plan    새로 적용할 플랜
     * @param today   기준일
     */
    @Transactional
    public void regenerateFrom(PlanProcess process, Plan plan, LocalDate today) {
        dailyChecklistRepository.deleteByProcess_IdAndTargetDateGreaterThan(process.getId(), today);
        generate(process, plan, today.plusDays(1));
    }

    private void addWeek(List<DailyChecklist> checklists, PlanProcess process, PlanTimeline timeline,
                         int week, LocalDate from) {
        LocalDate weekStart = process.getStartedAt().plusWeeks(week - 1L);

        for (int day = 0; day < DAYS_PER_WEEK; day++) {
            LocalDate targetDate = weekStart.plusDays(day);

            if (targetDate.isBefore(from)) {
                continue;
            }

            checklists.add(DailyChecklist.builder()
                    .process(process)
                    .timeline(timeline)
                    .content(timeline.getItem().getName())
                    .targetDate(targetDate)
                    .build());
        }
    }

    private Map<Long, List<Integer>> groupWeeks(List<PlanTimeline> timelines) {
        List<Long> timelineIds = timelines.stream().map(PlanTimeline::getId).toList();

        return planTimelineWeekRepository.findAllByTimeline_IdInOrderByWeekAsc(timelineIds).stream()
                .collect(Collectors.groupingBy(
                        week -> week.getTimeline().getId(),
                        Collectors.mapping(PlanTimelineWeek::getWeek, Collectors.toList())));
    }
}
