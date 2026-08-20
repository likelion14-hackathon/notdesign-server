package com.likelionknu.notdesign.report.service;

import com.likelionknu.notdesign.diary.data.entity.DailyChecklist;
import com.likelionknu.notdesign.diary.data.entity.Diary;
import com.likelionknu.notdesign.diary.data.entity.DiaryTodo;
import com.likelionknu.notdesign.diary.data.repository.DailyChecklistRepository;
import com.likelionknu.notdesign.diary.data.repository.DiaryRepository;
import com.likelionknu.notdesign.diary.data.repository.DiaryTodoRepository;
import com.likelionknu.notdesign.plan.data.entity.PlanItemEffect;
import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import com.likelionknu.notdesign.plan.data.entity.PlanTimelineWeek;
import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import com.likelionknu.notdesign.plan.data.repository.PlanItemEffectRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineWeekRepository;
import com.likelionknu.notdesign.plan.exception.PlanItemEffectNotFoundException;
import com.likelionknu.notdesign.report.data.enums.Reliability;
import com.likelionknu.notdesign.report.service.AttributionCalculator.DailyCondition;
import com.likelionknu.notdesign.report.service.AttributionCalculator.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportAttributionEngine {
    private static final int DAYS_PER_WEEK = 7;


    private final PlanTimelineRepository planTimelineRepository;
    private final PlanTimelineWeekRepository planTimelineWeekRepository;
    private final PlanItemEffectRepository planItemEffectRepository;
    private final DailyChecklistRepository dailyChecklistRepository;
    private final DiaryTodoRepository diaryTodoRepository;
    private final DiaryRepository diaryRepository;

    public record Attribution(PlanTimeline timeline, ImprovementItem improvement, BigDecimal score,
                              BigDecimal contributionRate, Integer price, Integer costPerPoint,
                              Reliability reliability) {
    }

    public record WeeklyExecution(PlanTimeline timeline, List<Integer> plannedWeeks, List<Integer> doneWeeks) {
    }

    public List<Attribution> attribute(Long processId, Long planId, int elapsedWeeks, LocalDate measuredDate,
                                       Map<ImprovementItem, Integer> deltas) {
        List<PlanTimeline> timelines = planTimelineRepository.findAllByPlan_IdOrderByItem_IdAsc(planId);

        if (timelines.isEmpty()) {
            return List.of();
        }

        List<Item> items = buildItems(processId, timelines, measuredDate);
        List<DailyCondition> conditions = buildConditions(processId, measuredDate);

        Map<Long, PlanTimeline> timelineById = new HashMap<>();

        for (PlanTimeline timeline : timelines) {
            timelineById.put(timeline.getId(), timeline);
        }

        List<Attribution> attributions = new ArrayList<>();

        for (AttributionCalculator.Attribution result
                : AttributionCalculator.attribute(items, deltas, elapsedWeeks, conditions)) {
            attributions.add(new Attribution(
                    timelineById.get(result.timelineId()),
                    result.improvement(),
                    result.score(),
                    result.contributionRate(),
                    result.price(),
                    result.costPerPoint(),
                    result.reliability()));
        }

        return attributions;
    }

    public List<WeeklyExecution> getWeeklyExecutions(Long processId, Long planId, LocalDate startedAt,
                                                     int totalWeeks, LocalDate until) {
        List<PlanTimeline> timelines = planTimelineRepository.findAllByPlan_IdOrderByItem_IdAsc(planId);
        Map<Long, List<Integer>> weeksByTimeline = getWeeks(timelines);
        Map<Long, Map<LocalDate, Boolean>> checklistByTimeline = getChecklists(processId, until);

        int elapsedWeeks = (int) (ChronoUnit.DAYS.between(startedAt, until) / DAYS_PER_WEEK);
        List<WeeklyExecution> executions = new ArrayList<>();

        for (PlanTimeline timeline : timelines) {
            List<Integer> planned = weeksByTimeline.getOrDefault(timeline.getId(), List.of());
            List<Integer> doneWeeks = new ArrayList<>();

            if (timeline.getItem().getCategory() == PlanCategory.PROCEDURE) {
                for (Integer week : planned) {
                    if (week <= elapsedWeeks && week <= totalWeeks) {
                        doneWeeks.add(week);
                    }
                }
            } else {
                Map<Integer, int[]> countsByWeek = countByWeek(
                        checklistByTimeline.getOrDefault(timeline.getId(), Map.of()), startedAt);

                for (int week = 1; week <= totalWeeks; week++) {
                    if (isWeekDone(countsByWeek.get(week))) {
                        doneWeeks.add(week);
                    }
                }
            }

            executions.add(new WeeklyExecution(timeline, planned, doneWeeks));
        }

        return executions;
    }

    private List<Item> buildItems(Long processId, List<PlanTimeline> timelines, LocalDate measuredDate) {
        Map<Long, List<Integer>> weeksByTimeline = getWeeks(timelines);
        Map<Long, Map<LocalDate, Boolean>> checklistByTimeline = getChecklists(processId, measuredDate);
        Map<String, Map<ImprovementItem, BigDecimal>> weightsByName = getWeights(timelines);

        List<Item> items = new ArrayList<>();

        for (PlanTimeline timeline : timelines) {
            items.add(new Item(
                    timeline.getId(),
                    timeline.getItem().getCategory(),
                    timeline.getItem().getPrice(),
                    weeksByTimeline.getOrDefault(timeline.getId(), List.of()),
                    weightsByName.get(timeline.getItem().getName()),
                    checklistByTimeline.getOrDefault(timeline.getId(), Map.of())));
        }

        return items;
    }

    private List<DailyCondition> buildConditions(Long processId, LocalDate measuredDate) {
        List<Diary> diaries = diaryRepository
                .findAllByProcess_IdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(
                        processId, measuredDate.atTime(23, 59, 59));

        List<DailyCondition> conditions = new ArrayList<>();

        for (Diary diary : diaries) {
            Map<ImprovementItem, Integer> badness = new EnumMap<>(ImprovementItem.class);

            if (diary.getSkinTone() != null) {
                badness.put(ImprovementItem.PIGMENTATION, 10 - diary.getSkinTone());
            }
            if (diary.getPores() != null) {
                badness.put(ImprovementItem.PORES, diary.getPores());
            }
            if (diary.getFlushing() != null) {
                badness.put(ImprovementItem.ERYTHEMA, diary.getFlushing());
            }

            conditions.add(new DailyCondition(diary.getRecordedAt().toLocalDate(), badness));
        }

        return conditions;
    }

    private Map<Long, List<Integer>> getWeeks(List<PlanTimeline> timelines) {
        List<Long> timelineIds = new ArrayList<>();

        for (PlanTimeline timeline : timelines) {
            timelineIds.add(timeline.getId());
        }

        Map<Long, List<Integer>> weeksByTimeline = new HashMap<>();

        for (PlanTimelineWeek week : planTimelineWeekRepository.findAllByTimeline_IdInOrderByWeekAsc(timelineIds)) {
            weeksByTimeline.computeIfAbsent(week.getTimeline().getId(), key -> new ArrayList<>())
                    .add(week.getWeek());
        }

        return weeksByTimeline;
    }

    private Map<Long, Map<LocalDate, Boolean>> getChecklists(Long processId, LocalDate until) {
        List<DailyChecklist> checklists =
                dailyChecklistRepository.findAllByProcess_IdAndTargetDateLessThanEqual(processId, until);
        Set<LocalDate> recordedDates = getRecordedDates(processId, until);

        Map<Long, Long> timelineByChecklist = new HashMap<>();
        Map<Long, Map<LocalDate, Boolean>> checklistByTimeline = new HashMap<>();
        Map<Long, LocalDate> dateByChecklist = new HashMap<>();
        List<Long> checklistIds = new ArrayList<>();

        for (DailyChecklist checklist : checklists) {
            // 하루 기록이 없는 날은 실행 여부를 알 수 없어 실행률 계산에서 제외한다.
            if (!recordedDates.contains(checklist.getTargetDate())) {
                continue;
            }

            Long timelineId = checklist.getTimeline().getId();

            timelineByChecklist.put(checklist.getId(), timelineId);
            dateByChecklist.put(checklist.getId(), checklist.getTargetDate());
            checklistByTimeline.computeIfAbsent(timelineId, key -> new HashMap<>())
                    .put(checklist.getTargetDate(), false);
            checklistIds.add(checklist.getId());
        }

        if (!checklistIds.isEmpty()) {
            for (DiaryTodo todo : diaryTodoRepository.findAllByChecklist_IdIn(checklistIds)) {
                if (Boolean.TRUE.equals(todo.getDone())) {
                    Long checklistId = todo.getChecklist().getId();

                    checklistByTimeline.get(timelineByChecklist.get(checklistId))
                            .put(dateByChecklist.get(checklistId), true);
                }
            }
        }

        return checklistByTimeline;
    }

    private Set<LocalDate> getRecordedDates(Long processId, LocalDate until) {
        Set<LocalDate> dates = new HashSet<>();

        for (Diary diary : diaryRepository
                .findAllByProcess_IdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(processId, until.atTime(23, 59, 59))) {
            dates.add(diary.getRecordedAt().toLocalDate());
        }

        return dates;
    }

    private Map<String, Map<ImprovementItem, BigDecimal>> getWeights(List<PlanTimeline> timelines) {
        List<String> names = new ArrayList<>();

        for (PlanTimeline timeline : timelines) {
            names.add(timeline.getItem().getName());
        }

        Map<String, Map<ImprovementItem, BigDecimal>> weightsByName = new HashMap<>();

        for (PlanItemEffect effect : planItemEffectRepository.findAllByNameIn(names)) {
            weightsByName.computeIfAbsent(effect.getName(), key -> new EnumMap<>(ImprovementItem.class))
                    .put(effect.getImprovement(), effect.getExpectedWeight());
        }

        for (String name : names) {
            if (!weightsByName.containsKey(name)) {
                throw new PlanItemEffectNotFoundException(name);
            }
        }

        return weightsByName;
    }

    private Map<Integer, int[]> countByWeek(Map<LocalDate, Boolean> checklist, LocalDate startedAt) {
        Map<Integer, int[]> countsByWeek = new HashMap<>();

        for (Map.Entry<LocalDate, Boolean> entry : checklist.entrySet()) {
            int week = (int) (ChronoUnit.DAYS.between(startedAt, entry.getKey()) / DAYS_PER_WEEK) + 1;
            int[] counts = countsByWeek.computeIfAbsent(week, key -> new int[2]);

            counts[0]++;

            if (Boolean.TRUE.equals(entry.getValue())) {
                counts[1]++;
            }
        }

        return countsByWeek;
    }

    private boolean isWeekDone(int[] counts) {
        return counts != null && counts[1] * 2 >= counts[0] && counts[0] > 0;
    }
}
