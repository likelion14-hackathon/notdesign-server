package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.service.PlanCatalogService.Catalog;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.CatalogEntry;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class PlanPromptBuilder {

    // 4개 모드 공용 규칙. 뒤에 "# 사용 가능한 관리 항목"으로 카탈로그를 이어 붙인다.
    private static final String SYSTEM_RULES = """
            # 역할

            너는 피부 관리 플랜을 설계하는 전문가다. 사용자의 피부 측정 결과를 보고 12주 관리 플랜을 만든다.

            이 플랜은 12주 뒤에 "무엇이 실제로 효과가 있었는지"를 지표별 기여도로 분리하는 데 쓰인다.
            효과가 좋은 조합을 짜는 것만으로는 부족하고, 원인을 분리할 수 있게 설계해야 한다.

            너는 항목을 고르고, 언제 할지 정하고, 왜인지 설명한다.
            이름·카테고리·가격·금액 계산은 하지 않는다. 목록의 id 로 지정하면 서버가 채운다.

            # 서비스 맥락

            0주 클리닉 측정 → 12주 플랜 시작 → 매일 30초 기록 → 6주차 중간 측정 → 기여도 리포트 → 다음 12주 사이클

            - 관리 항목은 AAC 브랜드가 실제로 제공하는 것들이다 (더나·엠레드 클리닉의 시술, 피쓰의 홈케어).
            - 12주 안에 모든 문제를 해결하려 하지 마라. 다음 사이클이 이어진다.
              이번 사이클에서 무엇이 효과였는지 깨끗하게 알아내는 것이 더 중요하다.
            - 6주차에 중간 측정과 중간 리포트가 있다. 1순위 지표를 겨냥한 항목은 6주 안에 신호가 잡히도록
              배치한다. 12주차에만 효과가 보이면 중간 리포트가 비어버린다.
            - LIFESTYLE·HOME_CARE·SUPPLEMENT 는 매일 체크리스트에 뜨고, 그 실행률이 기여도 계산에
              직접 들어간다. 지키기 어려운 항목은 실행률이 낮아져 효과가 있어도 신호가 안 잡힌다.
            - PROCEDURE 는 클리닉에서 받는 것이라 체크리스트에 뜨지 않는다.

            # 지표 읽는 법

            세 지표는 좋고 나쁨의 방향이 다르다. 우선순위는 "나쁜 정도"로 환산해서 비교한다.

            - 색소침착 나쁜 정도 = pigmentation      (낮을수록 좋은 지표)
            - 홍조 나쁜 정도 = erythema             (낮을수록 좋은 지표)
            - 수분력 나쁜 정도 = 100 - hydration     (높을수록 좋은 지표)

            환산값이 가장 큰 지표가 1순위, 두 번째가 2순위다.
            숫자 크기만 보고 비교하지 마라. 수분력은 반드시 100 에서 뺀 뒤에 비교한다.

            예) 색소침착 62, 수분력 55, 홍조 41 이면
                나쁜 정도는 색소침착 62 > 수분력 45 > 홍조 41 이므로
                1순위는 색소침착, 2순위는 수분력이다.

            # 항목 구성

            정확히 아래 개수로 구성한다.

            - PROCEDURE: 1개 또는 2개
            - 매일 체크하는 항목(LIFESTYLE + HOME_CARE + SUPPLEMENT): 2개 또는 3개
            - 합계 3~5개, 서로 다른 카테고리 3종류 이상
            - 같은 id 를 두 번 쓰지 않는다

            # 비용

            예산을 따로 받지 않는다. 아래 기준으로 판단한다.

            - "웰니스 지출 진단" 블록이 있으면, 최근 1년 지출의 월평균을 넘지 않게 고른다.
              이 서비스는 쓰던 돈을 늘리는 게 아니라 낭비를 걷어내는 것이 목적이다.
            - 그 블록이 없으면 PROCEDURE 를 1개만 넣고, 같은 효과라면 더 싼 항목을 고른다.
            - 가장 비싼 항목을 고르지 마라. 목록에서 1순위 지표 가중치가 같다면 가격이 낮은 쪽을 택한다.

            # 배치 규칙 (가장 중요)

            같은 주차에 모든 항목을 시작하면 12주 뒤에 무엇이 효과였는지 수학적으로 분리할 수 없다.
            시차가 비슷한 항목끼리는 시작 주차를 반드시 어긋나게 배치한다.

            카테고리별 기준:

            - PROCEDURE   효과 2~3주 후 계단식. 1차시 시작. weeks = 시술받는 주차, 3~4주 간격 (예: [1,4,8]).
                          마지막 시술은 8주차를 넘기지 않는다. 12주 측정에 효과가 온전히 반영되려면
                          마지막 시술과 측정 사이에 최소 4주가 필요하다.
            - LIFESTYLE   효과 3~5일, 즉시.     1차시 시작. weeks = 시작주차부터 12주까지 연속.
            - SUPPLEMENT  효과 3~4주 후 완만.   2주차 또는 3주차 시작. weeks = 시작주차부터 12주까지 연속.
            - HOME_CARE   효과 4~6주 후 완만.   4주차 시작. weeks = 4주부터 12주까지 연속.

            # 반드시 지킬 것

            - 1순위 지표를 겨냥한 항목은 1주차에 시작한다.
            - 같은 지표를 겨냥한 항목 두 개를 같은 주차에 시작하지 않는다.
              (색소를 노리는 시술과 홈케어를 둘 다 1주차에 넣으면 둘의 기여도를 나눌 수 없다)
            - 어떤 항목이 어떤 지표를 겨냥하는지는 목록의 가중치로 판단한다. 0.40 이상이면 그 지표를 겨냥한 것이다.

            # 문장 작성

            reason: 왜 이 항목인지 + 왜 이 주차인지를 사용자 말투로 한 문장.
            - 좋음: "시술과 구분되게 4주차부터 시작했어요"
            - 좋음: "색소 지표가 가장 낮아 우선적으로 배치했어요"
            - 나쁨: "피부에 좋습니다" (이유 없음)
            - 나쁨: "나이아신아마이드가 멜라닌 생성을 억제합니다" (성분 설명 불필요)

            frequency: 실행 주기. 예 "4주 간격 3회", "매일", "매일 1회", "아침저녁 2회"

            planSummary: 1순위와 2순위 지표가 드러나는 한 문장, 20자 내외.
            - 예: "색소 개선을 1순위로, 수분 부족을 함께 개선해요"

            의료 서비스가 아니다. 진단하지 않고 질환명을 쓰지 않는다.
            "치료", "완치", "개선 보장" 대신 "예상돼요", "기대돼요" 같은 추정 표현을 쓴다.

            # 작업 순서

            1. 위 환산식으로 세 지표의 나쁜 정도를 구하고 1순위와 2순위를 정한다.
            2. 목록에서 1순위 지표 가중치가 가장 높은 PROCEDURE 를 고르고 1주차에 배치한다.
            3. 2순위 지표를 겨냥하되 1순위와 시작 주차가 겹치지 않는 항목을 고른다.
            4. 매일 체크 항목을 2~3개 채운다. 매일 지키기 쉬운 것을 우선한다.
            5. 비용 기준으로 총액을 확인한다. 넘거나 과하면 더 싼 대안으로 교체한다.
            6. 출력 전에 확인한다:
               - 시작 주차가 같은 항목 중, 같은 지표(가중치 0.40 이상)를 겨냥하는 쌍이 있는가? 있으면 하나를 미룬다.
               - PROCEDURE 가 1~2개, 매일 체크 항목이 2~3개인가?
               - 모든 itemEffectId 가 목록에 있는가?

            # 사용 가능한 관리 항목

            itemEffectId 는 아래 목록의 id 다.
            pigmentation / hydration / erythema 는 그 항목이 해당 지표에 얼마나 강하게 작용하는지다 (0.00~0.60).
            """;

    /**
     * 4개 모드 공용 System 프롬프트. 규칙 뒤에 WIDE 카탈로그(JSON)를 이어 붙인다.
     *
     * @param catalog 피벗된 카탈로그
     * @return 규칙 + 카탈로그가 담긴 시스템 프롬프트
     */
    public String buildSystem(Catalog catalog) {
        return SYSTEM_RULES + "\n" + serializeCatalog(catalog);
    }

    /**
     * NEW 모드 User 프롬프트. 측정 결과와(있다면) 웰니스 지출 진단을 담는다.
     *
     * @param pigmentation 색소침착 측정값 (0~100)
     * @param hydration    수분력 측정값 (0~100)
     * @param erythema     홍조 측정값 (0~100)
     * @param monthlyBudget 월 평균 피부 관리 지출(원). null 이면 지출 진단 블록을 넣지 않는다.
     * @return NEW 모드 사용자 프롬프트
     */
    public String buildUserNew(int pigmentation, int hydration, int erythema, Integer monthlyBudget) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 모드\nNEW\n\n");
        builder.append("# 측정 결과\n")
                .append("- 색소침착 ").append(pigmentation).append("/100\n")
                .append("- 수분력 ").append(hydration).append("/100\n")
                .append("- 홍조 ").append(erythema).append("/100\n");

        if (monthlyBudget != null) {
            builder.append("\n# 웰니스 지출 진단\n")
                    .append("- 월 평균 피부 관리 지출: ").append(monthlyBudget).append("원\n");
        }

        return builder.toString();
    }

    public record MetricChange(String name, int before, int after, int delta) {
    }

    public record AttributionLine(String improvementName, BigDecimal score,
                                  BigDecimal contributionRate, String reliability) {
    }

    public record PreviousItem(long catalogId, String name, String categoryName,
                               List<Integer> weeks, String frequency, List<AttributionLine> attributions) {
    }

    /**
     * NEXT 모드 User 프롬프트. 직전 사이클의 최종 측정과 항목별 기여도를 담아 다음 12주를 다시 설계하게 한다.
     *
     * @param metrics            지표별 기준선→최종 변화 (측정 결과 겸 성과)
     * @param previousItems      지난 플랜 항목과 항목별 기여도
     * @param previousTotalPrice 지난 사이클 플랜 총액(원)
     * @param nextPlanPrice      낭비 제거 후 권장 총액(원). null 이면 지출 진단 블록을 넣지 않는다.
     * @return NEXT 모드 사용자 프롬프트
     */
    public String buildUserNext(List<MetricChange> metrics, List<PreviousItem> previousItems,
                                int previousTotalPrice, Integer nextPlanPrice) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 모드\nNEXT\n\n");
        builder.append("# 상황\n")
                .append("직전 12주 사이클이 끝났고 최종 측정까지 마쳤다.\n")
                .append("아래 측정 결과가 이번 사이클의 새 기준선이다. 이 값으로 1순위·2순위를 다시 정하고\n")
                .append("12주 플랜을 처음부터 다시 설계한다. 단, 지난 사이클에서 확인된 기여도를 반영한다.\n\n");

        builder.append("# 측정 결과\n");
        for (MetricChange metric : metrics) {
            builder.append("- ").append(metric.name()).append(" ").append(metric.after()).append("/100\n");
        }

        builder.append("\n# 지난 사이클 성과 (기준선 → 최종, 변화량)\n");
        for (MetricChange metric : metrics) {
            builder.append(describeMetric(metric));
        }

        builder.append("\n# 지난 플랜 구성과 항목별 기여도 (서버 계산 완료)\n")
                .append("id 는 목록의 itemEffectId 다.\n");
        for (PreviousItem item : previousItems) {
            builder.append(describePreviousItem(item));
        }

        builder.append("\n# 지난 성과 반영 규칙\n")
                .append("- 세 지표 모두에서 기여 신호가 없던 항목은 이번 플랜에 넣지 않는다.\n")
                .append("  그 항목이 겨냥하던 지표는 목록에서 다른 항목을 골라 채운다.\n")
                .append("- 어느 지표든 기여율 10% 이상이 확인된 항목은 유지를 기본으로 한다.\n")
                .append("  유지하더라도 이번 사이클은 새 12주다. 주차는 배치 규칙대로 처음부터 다시 짠다.\n")
                .append("- 새 기준선에서 1순위 지표가 지난 사이클과 달라졌으면, 지난 항목 유지보다 새 1순위 공략을 우선한다.\n");

        if (nextPlanPrice != null) {
            builder.append("\n# 웰니스 지출 진단\n")
                    .append("- 지난 사이클 플랜 총액: ").append(previousTotalPrice).append("원\n")
                    .append("- 낭비 제거 후 권장 총액: ").append(nextPlanPrice).append("원\n")
                    .append("- 이번 플랜 총액은 권장 총액을 넘지 않게 고른다.\n");
        }

        return builder.toString();
    }

    private String describeMetric(MetricChange metric) {
        return "- %s %d → %d (%+d점)%n".formatted(metric.name(), metric.before(), metric.after(), metric.delta());
    }

    private String describePreviousItem(PreviousItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append("- [id ").append(item.catalogId()).append("] ").append(item.name())
                .append(" (").append(item.categoryName()).append(") · 주차 ").append(item.weeks())
                .append(" · ").append(item.frequency()).append("\n");
        for (AttributionLine line : item.attributions()) {
            builder.append(describeAttribution(line));
        }
        return builder.toString();
    }

    private String describeAttribution(AttributionLine line) {
        if (line.score() == null || line.score().signum() == 0) {
            return "  · %s: 기여 신호 없음%n".formatted(line.improvementName());
        }
        return "  · %s: %s점, 기여율 %s%%, 신뢰도 %s%n".formatted(
                line.improvementName(),
                line.score().stripTrailingZeros().toPlainString(),
                line.contributionRate().stripTrailingZeros().toPlainString(),
                line.reliability());
    }

    private String serializeCatalog(Catalog catalog) {
        StringBuilder builder = new StringBuilder("[\n");
        List<CatalogEntry> entries = catalog.entries();
        for (int index = 0; index < entries.size(); index++) {
            CatalogEntry entry = entries.get(index);
            builder.append("  {\"id\": ").append(entry.id())
                    .append(", \"name\": \"").append(entry.name())
                    .append("\", \"category\": \"").append(entry.category().name())
                    .append("\", \"price\": ").append(entry.price())
                    .append(", \"pigmentation\": ").append(entry.pigmentation().toPlainString())
                    .append(", \"hydration\": ").append(entry.hydration().toPlainString())
                    .append(", \"erythema\": ").append(entry.erythema().toPlainString())
                    .append("}");
            builder.append(index < entries.size() - 1 ? ",\n" : "\n");
        }
        builder.append("]");
        return builder.toString();
    }
}
