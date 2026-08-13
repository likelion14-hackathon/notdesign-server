package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.entity.PlanItemEffect;
import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import com.likelionknu.notdesign.plan.data.repository.PlanItemEffectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanCatalogService {
    private final PlanItemEffectRepository planItemEffectRepository;

    public record CatalogEntry(Long id, String name, PlanCategory category, int price,
                               BigDecimal pigmentation, BigDecimal hydration, BigDecimal erythema) {
    }

    public record Catalog(List<CatalogEntry> entries, Map<Long, CatalogEntry> byAnyId,
                          Map<String, CatalogEntry> byName) {
        public boolean containsId(Long id) {
            return byAnyId.containsKey(id);
        }

        public CatalogEntry resolve(Long id) {
            return byAnyId.get(id);
        }

        public CatalogEntry resolveByName(String name) {
            return byName.get(name);
        }
    }

    /**
     * item_effect 전체를 한 번 조회해 WIDE 카탈로그로 피벗한다.
     *
     * @return 프롬프트용 목록 + id 역조회 맵
     */
    @Transactional(readOnly = true)
    public Catalog load() {
        List<PlanItemEffect> all = planItemEffectRepository.findAll();

        Map<String, List<PlanItemEffect>> byName = all.stream()
                .collect(Collectors.groupingBy(PlanItemEffect::getName));

        List<CatalogEntry> entries = new ArrayList<>();
        Map<String, CatalogEntry> entryByName = new HashMap<>();
        for (List<PlanItemEffect> group : byName.values()) {
            CatalogEntry entry = toEntry(group);
            entries.add(entry);
            entryByName.put(entry.name(), entry);
        }
        entries.sort(Comparator.comparing(CatalogEntry::id));

        Map<Long, CatalogEntry> byAnyId = all.stream()
                .collect(Collectors.toMap(PlanItemEffect::getId, effect -> entryByName.get(effect.getName())));

        return new Catalog(entries, byAnyId, entryByName);
    }

    private CatalogEntry toEntry(List<PlanItemEffect> group) {
        PlanItemEffect any = group.get(0);
        Long id = group.stream().map(PlanItemEffect::getId).min(Long::compareTo).orElseThrow();

        Map<ImprovementItem, BigDecimal> weights = group.stream()
                .collect(Collectors.toMap(PlanItemEffect::getImprovement, PlanItemEffect::getExpectedWeight, (a, b) -> a));

        return new CatalogEntry(
                id,
                any.getName(),
                any.getCategory(),
                any.getPrice(),
                weights.getOrDefault(ImprovementItem.PIGMENTATION, BigDecimal.ZERO),
                weights.getOrDefault(ImprovementItem.HYDRATION, BigDecimal.ZERO),
                weights.getOrDefault(ImprovementItem.ERYTHEMA, BigDecimal.ZERO));
    }
}
