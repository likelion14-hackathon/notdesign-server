package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.service.PlanCatalogService.Catalog;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.CatalogEntry;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class PlanPromptBuilder {

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

            세 지표는 모두 낮을수록 좋은 지표다. 값이 클수록 나쁘다. 값 그대로 "나쁜 정도"로 비교한다.

            - 색소침착 나쁜 정도 = pigmentation
            - 홍조 나쁜 정도 = erythema
            - 모공 나쁜 정도 = pores

            값이 가장 큰 지표가 1순위, 두 번째가 2순위다.

            예) 색소침착 62, 모공 55, 홍조 41 이면
                나쁜 정도는 색소침착 62 > 모공 55 > 홍조 41 이므로
                1순위는 색소침착, 2순위는 모공이다.

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
            - 예: "색소 개선을 1순위로, 모공을 함께 개선해요"

            의료 서비스가 아니다. 진단하지 않고 질환명을 쓰지 않는다.
            "치료", "완치", "개선 보장" 대신 "예상돼요", "기대돼요" 같은 추정 표현을 쓴다.

            # 작업 순서

            1. 위 방식으로 세 지표의 나쁜 정도를 구하고 1순위와 2순위를 정한다.
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
            pigmentation / pores / erythema 는 그 항목이 해당 지표에 얼마나 강하게 작용하는지다 (0.00~0.60).
            """;

    private static final String SYSTEM_TRIAL_RULES = """
            # 역할

            너는 피부 관리 플랜을 설계하는 전문가다. 사용자의 셀카 기반 간이 분석 결과를 보고
            1주짜리 체험 플랜을 만든다.

            이 플랜은 정식 12주 플랜의 맛보기다. 완벽한 개선이 아니라, 매일 기록하는 습관과
            관리 루틴을 부담 없이 경험하게 하는 데 집중한다.

            너는 항목을 고르고 왜인지 설명한다.
            이름·카테고리·가격·금액 계산은 하지 않는다. 목록의 id 로 지정하면 서버가 채운다.

            # 서비스 맥락

            셀카 간이 분석 → 1주 체험 플랜 → 매일 30초 기록 → 체험 종료 후 정식 12주 플랜 제안

            - 관리 항목은 AAC 브랜드가 실제로 제공하는 것들이다 (더나·엠레드 클리닉의 시술, 피쓰의 홈케어).
            - 1주 안에 피부는 바뀌지 않는다. 바꾸려 하지 마라. 매일 지키기 쉬운 항목으로
              "기록하는 경험"을 만들어 주는 것이 목표다.
            - LIFESTYLE·HOME_CARE·SUPPLEMENT 는 매일 체크리스트에 뜬다. 지키기 어려운 항목은
              체험 만족도를 떨어뜨린다.
            - PROCEDURE 는 클리닉에서 받는 것이라 체크리스트에 뜨지 않는다.
              체험에서는 가장 급한 지표를 겨냥한 시술 1개를 가볍게 받아보는 제안으로만 넣는다.

            # 지표 읽는 법

            간이 분석 결과는 세 지표 모두 "나쁜 정도"로 0~100 환산되어 온다. 값이 클수록 나쁘다.
            방향 환산은 이미 되어 있으니 크기만 비교하면 된다.

            - 색소침착 나쁜 정도 / 모공 나쁜 정도 / 홍조 나쁜 정도
            - 가장 큰 값이 1순위, 두 번째가 2순위다.
            - 목록의 pigmentation / pores / erythema 가중치가 각각 색소침착 / 모공 / 홍조에 대응한다.

            # 항목 구성

            정확히 아래 개수로 구성한다.

            - PROCEDURE: 정확히 1개 (1순위 지표 가중치가 높으면서 가격이 낮은 것)
            - 매일 체크하는 항목(LIFESTYLE + HOME_CARE + SUPPLEMENT): 2개 또는 3개
            - 합계 3~4개, 서로 다른 카테고리 3종류 이상
            - 같은 id 를 두 번 쓰지 않는다

            # 비용

            체험이다. 비용 부담이 크면 체험의 의미가 없다.

            - PROCEDURE 는 같은 지표를 겨냥한 항목 중 가격이 낮은 쪽을 고른다. 가장 비싼 항목은 고르지 않는다.
            - 매일 체크 항목(LIFESTYLE·HOME_CARE·SUPPLEMENT)도 같은 효과라면 더 싼 항목을 고른다. 특히 HOME_CARE 는 고가 제품을 피한다.
            - SUPPLEMENT 는 1주 만에 효과가 나지 않으므로, 넣더라도 저가일 때만 1개 넣는다.
            - 체험 총액은 최대한 낮춘다. 정식 12주 플랜보다 비싸면 안 된다. 한 주 부담 없이 해보는 수준으로 구성한다.

            # 배치 규칙

            1주 플랜이므로 모든 항목의 weeks 는 반드시 [1] 하나다. [1] 외의 값을 쓰지 마라.
            12주 플랜의 간격 규칙은 여기 적용되지 않는다.

            - PROCEDURE   weeks = [1], frequency 는 "1회"
            - LIFESTYLE / HOME_CARE / SUPPLEMENT   weeks = [1], frequency 는 "매일", "매일 1회", "아침저녁 2회" 등

            # 문장 작성

            reason: 왜 이 항목인지를 체험자 눈높이의 사용자 말투로 한 문장.
            - 좋음: "홍조 지표가 가장 높아 이번 주에 가볍게 받아보길 추천해요"
            - 좋음: "하루 한 번이면 되니 체험 기간에 부담 없이 지킬 수 있어요"
            - 나쁨: "피부에 좋습니다" (이유 없음)

            frequency: 1주 안에서의 실행 주기. 예 "1회", "매일", "매일 1회", "아침저녁 2회"
            "4주 간격 3회" 같은 여러 주에 걸친 표현은 쓰지 않는다.

            planSummary: 1순위 지표와 "1주 체험"이 드러나는 한 문장, 20자 내외.
            - 예: "홍조 완화를 중심으로 한 주 가볍게 체험해요"

            의료 서비스가 아니다. 진단하지 않고 질환명을 쓰지 않는다.
            1주 만에 개선된다는 표현을 쓰지 않는다. "경험해 봐요", "습관을 만들어 봐요", "기대돼요" 같은 표현을 쓴다.

            # 작업 순서

            1. 세 지표의 나쁜 정도를 크기순으로 비교해 1순위와 2순위를 정한다.
            2. 목록에서 1순위 지표 가중치가 높고 가격이 낮은 PROCEDURE 를 1개 고른다.
            3. 매일 지키기 쉬운 체크 항목을 2~3개 고른다. 1순위·2순위 지표를 하나씩은 겨냥한다.
            4. 출력 전에 확인한다:
               - 모든 항목의 weeks 가 정확히 [1] 인가?
               - PROCEDURE 가 정확히 1개, 매일 체크 항목이 2~3개, 합계 3~4개인가?
               - 서로 다른 카테고리가 3종류 이상인가?
               - 모든 itemEffectId 가 목록에 있는가?

            # 사용 가능한 관리 항목

            itemEffectId 는 아래 목록의 id 다.
            pigmentation / pores / erythema 는 그 항목이 해당 지표에 얼마나 강하게 작용하는지다 (0.00~0.60).
            """;

    public String buildSystem(Catalog catalog) {
        return SYSTEM_RULES + "\n" + serializeCatalog(catalog);
    }

    public String buildSystemTrial(Catalog catalog) {
        return SYSTEM_TRIAL_RULES + "\n" + serializeCatalog(catalog);
    }

    public String buildUserNew(int pigmentation, int pores, int erythema, Integer monthlyBudget) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 모드\nNEW\n\n");
        builder.append("# 측정 결과\n")
                .append("- 색소침착 ").append(pigmentation).append("/100\n")
                .append("- 모공 ").append(pores).append("/100\n")
                .append("- 홍조 ").append(erythema).append("/100\n");

        if (monthlyBudget != null) {
            builder.append("\n# 웰니스 지출 진단\n")
                    .append("- 월 평균 피부 관리 지출: ").append(monthlyBudget).append("원\n");
        }

        return builder.toString();
    }

    public String buildUserTrial(double skinTone, double pores, double redness) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 모드\nTRIAL (1주 체험)\n\n");
        builder.append("# 셀카 간이 분석 결과 (나쁜 정도, 0~100, 클수록 나쁨)\n")
                .append("- 색소침착 나쁜 정도 ").append(Math.round((10 - skinTone) * 10)).append("/100\n")
                .append("- 모공 나쁜 정도 ").append(Math.round(pores * 10)).append("/100\n")
                .append("- 홍조 나쁜 정도 ").append(Math.round(redness * 10)).append("/100\n");

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

    public record ExecutionLine(String name, List<Integer> plannedWeeks, List<Integer> doneWeeks) {
    }

    public String buildUserAdjust(List<MetricChange> metrics, int elapsedWeeks,
                                  List<PreviousItem> currentItems, List<ExecutionLine> executions) {
        int adjustFrom = elapsedWeeks + 1;
        StringBuilder builder = new StringBuilder();
        builder.append("# 모드\nADJUST\n\n");
        builder.append("# 상황\n")
                .append(elapsedWeeks).append("주차에 중간 측정을 마친, 진행 중인 12주 사이클이다.\n")
                .append("지금 만드는 플랜은 진행 중인 사이클에 그대로 덮어씌워진다. 시작일과 진행률은 바뀌지 않는다.\n")
                .append("1~").append(elapsedWeeks).append("주차는 이미 지나간 시간이라 수정할 수 없다.\n")
                .append("출력은 여전히 1~12주차 전체를 담은 12주 플랜이다.\n\n");

        builder.append("# 중간 측정 결과\n");
        for (MetricChange metric : metrics) {
            builder.append("- ").append(metric.name()).append(" ").append(metric.after()).append("/100\n");
        }

        builder.append("\n# 지표 변화 (기준선 → 중간 측정, 변화량)\n");
        for (MetricChange metric : metrics) {
            builder.append(describeMetric(metric));
        }

        builder.append("\n# 현재 플랜 구성과 항목별 기여도 (복제 원본, 서버 계산 완료)\n")
                .append("id 는 목록의 itemEffectId 다. 주차는 현재 배치 그대로다.\n");
        for (PreviousItem item : currentItems) {
            builder.append(describePreviousItem(item));
        }

        builder.append("\n# 주차별 실행 현황 (측정일까지)\n");
        for (ExecutionLine execution : executions) {
            builder.append(describeExecution(execution));
        }

        builder.append("\n# 조정 규칙 (반드시 지킬 것)\n")
                .append("1. 1~").append(elapsedWeeks).append("주차 복제: 위 항목을 모두 출력에 포함하고, 각 항목 주차 중 ")
                .append(elapsedWeeks).append(" 이하 값은 원본과 완전히 같아야 한다. 빼지도 더하지도 바꾸지도 않는다.\n")
                .append("2. 조정은 ").append(adjustFrom).append("주차부터만 한다.\n")
                .append("   - 세 지표 모두 기여 신호가 없던 항목은 주차에서 ").append(adjustFrom)
                .append("주차 이후를 제거해 중단한다. 그 전 주차는 1번대로 남긴다.\n")
                .append("   - 기여가 확인된 항목은 ").append(adjustFrom).append("~12주차 배치를 원본 흐름대로 이어간다.\n")
                .append("3. 중단한 항목 대신 같은 지표를 겨냥하는 새 항목을 ").append(adjustFrom)
                .append("주차부터 투입할 수 있다. 단, 교체 후에도 전체가 항목 구성 규칙(합계 3~5개, PROCEDURE 1~2개, 매일 체크 2~3개, 카테고리 3종 이상, id 중복 금지)을 만족해야 한다. 이미 5개면 투입 없이 중단만 한다.\n")
                .append("4. 새로 투입하는 PROCEDURE 는 마지막 시술이 8주차를 넘지 않아야 한다. 불가능하면 매일 체크 항목으로 대체한다.\n")
                .append("5. 실행률이 낮아 신호가 안 잡힌 항목(실행 주차가 계획의 절반 미만)은 효과가 없다고 단정하지 않는다. 중단 대신 유지하고 reason 에 실행을 독려하는 문장을 쓴다.\n")
                .append("6. \"1순위 항목은 1주차에 시작\" 규칙은 이미 지난 구간이라 적용하지 않는다. 시차 분리 규칙은 ")
                .append(adjustFrom).append("주차 이후 새로 시작하는 항목에만 적용한다.\n");

        builder.append("\n# 문장 작성\n")
                .append("- 유지 항목 reason 은 유지 이유가 드러나게 쓴다. 예: \"색소 기여율이 확인돼 그대로 이어가요\"\n")
                .append("- 실행 기록이 없거나 실행률이 낮은 항목의 reason 에는 기여가 확인됐다는 표현을 쓰지 않는다. 실행이 없으면 기여 신호도 없으므로 실행을 독려하는 내용만 쓴다.\n")
                .append("- 중단·투입 항목 reason 은 왜 ").append(adjustFrom)
                .append("주차부터인지가 드러나야 한다. 예: \"신호가 잡히지 않아 7주차부터 다른 항목으로 바꿔 실험해요\"\n")
                .append("- planSummary 는 무엇을 실험하는지 드러나는 한 문장, 20자 내외. 예: \"7주차부터 홍조 항목을 교체해 실험해요\"\n");

        return builder.toString();
    }

    private String describeExecution(ExecutionLine execution) {
        return "- %s: 계획 %s 중 실행 %s%n".formatted(
                execution.name(), execution.plannedWeeks(), execution.doneWeeks());
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
                    .append(", \"pores\": ").append(entry.pores().toPlainString())
                    .append(", \"erythema\": ").append(entry.erythema().toPlainString())
                    .append("}");
            builder.append(index < entries.size() - 1 ? ",\n" : "\n");
        }
        builder.append("]");
        return builder.toString();
    }
}
