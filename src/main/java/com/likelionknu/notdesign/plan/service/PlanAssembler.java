package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse;
import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse.Item;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import com.likelionknu.notdesign.plan.data.entity.PlanTimelineWeek;
import com.likelionknu.notdesign.plan.data.enums.PlanGenerationMode;
import com.likelionknu.notdesign.plan.data.repository.PlanItemRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineRepository;
import com.likelionknu.notdesign.plan.data.repository.PlanTimelineWeekRepository;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.Catalog;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.CatalogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlanAssembler {
    private final PlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final PlanTimelineRepository planTimelineRepository;
    private final PlanTimelineWeekRepository planTimelineWeekRepository;

    /**
     * AI 응답을 Plan → PlanItem → PlanTimeline → PlanTimelineWeek 순으로 저장한다.
     *
     * @param mode     생성 모드 (PlanType·기간 결정)
     * @param response 검증을 통과한 AI 응답
     * @param catalog  itemEffectId 역조회용 카탈로그
     * @return 저장된 Plan
     */
    @Transactional
    public Plan assemble(PlanGenerationMode mode, PlanGenerationAiResponse response, Catalog catalog) {
        int totalPrice = response.items().stream()
                .mapToInt(item -> catalog.resolve(item.itemEffectId().longValue()).price())
                .sum();

        Plan plan = planRepository.save(Plan.builder()
                .type(mode.getPlanType())
                .durationWeeks(mode.getDurationWeeks())
                .summary(response.planSummary())
                .totalPrice(totalPrice)
                .build());

        for (Item item : response.items()) {
            CatalogEntry entry = catalog.resolve(item.itemEffectId().longValue());

            PlanItem planItem = planItemRepository.save(PlanItem.builder()
                    .category(entry.category())
                    .name(entry.name())
                    .frequency(item.frequency())
                    .price(entry.price())
                    .build());

            PlanTimeline timeline = planTimelineRepository.save(PlanTimeline.builder()
                    .plan(plan)
                    .item(planItem)
                    .reason(item.reason())
                    .build());

            List<PlanTimelineWeek> weeks = item.weeks().stream()
                    .map(week -> PlanTimelineWeek.builder().timeline(timeline).week(week).build())
                    .toList();
            planTimelineWeekRepository.saveAll(weeks);
        }

        return plan;
    }
}
