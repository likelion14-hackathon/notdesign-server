package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse;
import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse.Item;
import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.Catalog;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.CatalogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class PlanGenerationValidator {

    public List<String> validate(PlanGenerationAiResponse response, Catalog catalog, int durationWeeks) {
        List<String> violations = new ArrayList<>();
        List<Item> items = response.items();

        if (items.size() < 3 || items.size() > 6) {
            violations.add("항목 수는 3~6개여야 하는데 " + items.size() + "개입니다.");
        }

        Set<Long> seen = new HashSet<>();
        Set<PlanCategory> categories = EnumSet.noneOf(PlanCategory.class);
        int procedure = 0;
        int dailyCheck = 0;

        for (Item item : items) {
            Long id = item.itemEffectId() == null ? null : item.itemEffectId().longValue();
            if (id == null || !catalog.containsId(id)) {
                violations.add("itemEffectId " + item.itemEffectId() + " 가 카탈로그에 없습니다.");
                continue;
            }

            CatalogEntry entry = catalog.resolve(id);
            if (!seen.add(entry.id())) {
                violations.add("항목 '" + entry.name() + "' 이(가) 중복 선택되었습니다.");
            }

            categories.add(entry.category());
            if (entry.category() == PlanCategory.PROCEDURE) {
                procedure++;
            } else {
                dailyCheck++;
            }

            validateWeeks(entry, item, durationWeeks, violations);
        }

        if (procedure > 3) {
            violations.add("PROCEDURE 는 최대 3개여야 하는데 " + procedure + "개입니다.");
        }
        if (dailyCheck < 1 || dailyCheck > 4) {
            violations.add("매일 체크 항목(생활습관·홈케어·영양제)은 1~4개여야 하는데 " + dailyCheck + "개입니다.");
        }
        if (categories.size() < 2) {
            violations.add("서로 다른 카테고리를 2종류 이상 포함해야 합니다.");
        }

        return violations;
    }

    /**
     * 재요청 후에도 남으면 생성을 중단해야 하는 치명적 위반만 검출한다.
     * (카탈로그에 없는 itemEffectId·null weeks 는 이후 조립 단계에서 예외를 유발한다.)
     */
    public List<String> validateFatal(PlanGenerationAiResponse response, Catalog catalog) {
        List<String> violations = new ArrayList<>();

        for (Item item : response.items()) {
            Long id = item.itemEffectId() == null ? null : item.itemEffectId().longValue();
            if (id == null || !catalog.containsId(id)) {
                violations.add("itemEffectId " + item.itemEffectId() + " 가 카탈로그에 없습니다.");
                continue;
            }
            if (item.weeks() == null) {
                violations.add("항목 '" + catalog.resolve(id).name() + "' 의 weeks 가 비어 있습니다.");
            }
        }

        return violations;
    }

    private void validateWeeks(CatalogEntry entry, Item item, int durationWeeks, List<String> violations) {
        if (item.weeks() == null || item.weeks().isEmpty()) {
            violations.add("항목 '" + entry.name() + "' 의 weeks 가 비어 있습니다.");
            return;
        }

        for (Integer week : item.weeks()) {
            if (week == null || week < 1 || week > durationWeeks) {
                violations.add("항목 '" + entry.name() + "' 의 주차 " + week + " 가 1~" + durationWeeks + " 범위를 벗어났습니다.");
            }
        }
    }
}
