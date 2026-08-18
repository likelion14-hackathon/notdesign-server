package com.likelionknu.notdesign.report.service;

import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import com.likelionknu.notdesign.report.data.enums.Reliability;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributionCalculator {
    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final int MIN_EXPERIMENT_DAYS = 5;
    private static final double NO_SIGNAL_RATIO = 0.35;
    private static final double STRONG_SIGNAL_RATIO = 1.0;

    private static final BigDecimal HIGH_ADHERENCE = BigDecimal.valueOf(0.80);
    private static final BigDecimal MID_ADHERENCE = BigDecimal.valueOf(0.50);
    private static final int RELIABLE_WEEKS = 3;

    public record Item(Long timelineId, PlanCategory category, int price, List<Integer> weeks,
                       Map<ImprovementItem, BigDecimal> weights, Map<LocalDate, Boolean> checklist) {
    }

    public record DailyCondition(LocalDate date, Map<ImprovementItem, Integer> badness) {
    }

    public record Attribution(Long timelineId, ImprovementItem improvement, BigDecimal score,
                              BigDecimal contributionRate, int price, Integer costPerPoint,
                              Reliability reliability) {
    }

    public static List<Attribution> attribute(List<Item> items, Map<ImprovementItem, Integer> deltas,
                                              int elapsedWeeks, List<DailyCondition> conditions) {
        List<Attribution> attributions = new ArrayList<>();

        for (ImprovementItem improvement : ImprovementItem.activeValues()) {
            attributions.addAll(attributeMetric(items, improvement,
                    deltas.getOrDefault(improvement, 0), elapsedWeeks, conditions));
        }

        return attributions;
    }

    private static List<Attribution> attributeMetric(List<Item> items, ImprovementItem improvement, int delta,
                                                     int elapsedWeeks, List<DailyCondition> conditions) {
        Map<Long, BigDecimal> rawByItem = new HashMap<>();
        Map<Long, Double> experimentByItem = new HashMap<>();
        BigDecimal totalRaw = BigDecimal.ZERO;

        for (Item item : items) {
            double experiment = experimentFactor(item, improvement, conditions);
            BigDecimal raw = computeRaw(item, improvement, elapsedWeeks)
                    .multiply(BigDecimal.valueOf(experiment));

            rawByItem.put(item.timelineId(), raw);
            experimentByItem.put(item.timelineId(), experiment);
            totalRaw = totalRaw.add(raw);
        }

        List<BigDecimal> scores = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();

        for (Item item : items) {
            BigDecimal raw = rawByItem.get(item.timelineId());
            BigDecimal share = totalRaw.signum() == 0
                    ? BigDecimal.ZERO
                    : raw.divide(totalRaw, 6, RoundingMode.HALF_UP);

            scores.add(BigDecimal.valueOf(delta).multiply(share).setScale(SCALE, RoundingMode.HALF_UP));
            rates.add(share.multiply(HUNDRED).setScale(SCALE, RoundingMode.HALF_UP));
        }

        if (totalRaw.signum() != 0) {
            fixResidual(scores, BigDecimal.valueOf(delta), largestIndex(rawByItem, items));
            fixResidual(rates, HUNDRED, largestIndex(rawByItem, items));
        }

        List<Attribution> attributions = new ArrayList<>();

        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            BigDecimal score = scores.get(index);
            double experiment = experimentByItem.get(item.timelineId());
            int price = computePrice(item, elapsedWeeks);

            attributions.add(new Attribution(
                    item.timelineId(),
                    improvement,
                    score,
                    rates.get(index),
                    price,
                    computeCostPerPoint(price, score),
                    computeReliability(item, elapsedWeeks, score, experiment)));
        }

        return attributions;
    }

    private static BigDecimal computeRaw(Item item, ImprovementItem improvement, int elapsedWeeks) {
        BigDecimal weight = item.weights().getOrDefault(improvement, BigDecimal.ZERO);
        BigDecimal maturity = averageMaturity(item, elapsedWeeks);

        if (weight.signum() == 0 || maturity.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return weight.multiply(adherence(item)).multiply(maturity);
    }

    private static BigDecimal averageMaturity(Item item, int elapsedWeeks) {
        double sum = 0;
        int count = 0;

        for (Integer week : item.weeks()) {
            if (week <= elapsedWeeks) {
                sum += maturity(item.category(), elapsedWeeks - week);
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(sum / count).setScale(6, RoundingMode.HALF_UP);
    }

    private static double maturity(PlanCategory category, int weeksSince) {
        return switch (category) {
            case PROCEDURE -> weeksSince < 2 ? 0.0 : weeksSince == 2 ? 0.6 : 1.0;
            case LIFESTYLE -> 1.0;
            case SUPPLEMENT -> weeksSince < 3 ? 0.0 : weeksSince == 3 ? 0.5 : 1.0;
            case HOME_CARE -> weeksSince < 4 ? 0.0 : weeksSince == 4 ? 0.4 : weeksSince == 5 ? 0.7 : 1.0;
        };
    }

    private static BigDecimal adherence(Item item) {
        if (item.checklist().isEmpty()) {
            return BigDecimal.ONE;
        }

        int done = 0;

        for (Boolean checked : item.checklist().values()) {
            if (Boolean.TRUE.equals(checked)) {
                done++;
            }
        }

        return BigDecimal.valueOf(done)
                .divide(BigDecimal.valueOf(item.checklist().size()), 6, RoundingMode.HALF_UP);
    }

    private static double experimentFactor(Item item, ImprovementItem improvement,
                                           List<DailyCondition> conditions) {
        if (item.checklist().isEmpty() || conditions.isEmpty()) {
            return 1.0;
        }

        List<Integer> doneBadness = new ArrayList<>();
        List<Integer> missBadness = new ArrayList<>();

        for (DailyCondition condition : conditions) {
            Boolean checked = item.checklist().get(condition.date());
            Integer badness = condition.badness().get(improvement);

            if (checked == null || badness == null) {
                continue;
            }

            if (checked) {
                doneBadness.add(badness);
            } else {
                missBadness.add(badness);
            }
        }

        if (doneBadness.size() < MIN_EXPERIMENT_DAYS || missBadness.size() < MIN_EXPERIMENT_DAYS) {
            return 1.0;
        }

        double signal = mean(missBadness) - mean(doneBadness);
        double noise = stddev(conditions, improvement);

        if (signal < noise * NO_SIGNAL_RATIO) {
            return 0.0;
        }

        if (signal >= noise * STRONG_SIGNAL_RATIO) {
            return 1.2;
        }

        return 1.0;
    }

    private static int computePrice(Item item, int elapsedWeeks) {
        if (item.category() != PlanCategory.PROCEDURE) {
            return item.price();
        }

        int done = 0;

        for (Integer week : item.weeks()) {
            if (week <= elapsedWeeks) {
                done++;
            }
        }

        return item.price() * done;
    }

    private static Integer computeCostPerPoint(int price, BigDecimal score) {
        if (score.signum() == 0) {
            return null;
        }

        return BigDecimal.valueOf(price).divide(score.abs(), 0, RoundingMode.DOWN).intValue();
    }

    private static Reliability computeReliability(Item item, int elapsedWeeks, BigDecimal score,
                                                  double experiment) {
        if (score.signum() == 0) {
            return Reliability.LOW;
        }

        BigDecimal adherence = adherence(item);
        int matured = 0;

        for (Integer week : item.weeks()) {
            if (week <= elapsedWeeks && maturity(item.category(), elapsedWeeks - week) >= 1.0) {
                matured++;
            }
        }

        if (experiment > 1.0 && adherence.compareTo(MID_ADHERENCE) >= 0) {
            return Reliability.HIGH;
        }

        if (adherence.compareTo(HIGH_ADHERENCE) >= 0 && matured >= RELIABLE_WEEKS) {
            return Reliability.HIGH;
        }

        if (adherence.compareTo(MID_ADHERENCE) >= 0) {
            return Reliability.MID;
        }

        return Reliability.LOW;
    }

    private static void fixResidual(List<BigDecimal> values, BigDecimal target, int index) {
        BigDecimal sum = BigDecimal.ZERO;

        for (BigDecimal value : values) {
            sum = sum.add(value);
        }

        values.set(index, values.get(index).add(target.subtract(sum)));
    }

    private static int largestIndex(Map<Long, BigDecimal> rawByItem, List<Item> items) {
        int largest = 0;

        for (int index = 1; index < items.size(); index++) {
            BigDecimal current = rawByItem.get(items.get(index).timelineId());
            BigDecimal max = rawByItem.get(items.get(largest).timelineId());

            if (current.abs().compareTo(max.abs()) > 0) {
                largest = index;
            }
        }

        return largest;
    }

    private static double mean(List<Integer> values) {
        double sum = 0;

        for (Integer value : values) {
            sum += value;
        }

        return sum / values.size();
    }

    private static double stddev(List<DailyCondition> conditions, ImprovementItem improvement) {
        List<Integer> values = new ArrayList<>();

        for (DailyCondition condition : conditions) {
            Integer badness = condition.badness().get(improvement);

            if (badness != null) {
                values.add(badness);
            }
        }

        if (values.size() < 2) {
            return 1.0;
        }

        double mean = mean(values);
        double sum = 0;

        for (Integer value : values) {
            sum += (value - mean) * (value - mean);
        }

        return Math.max(0.5, Math.sqrt(sum / (values.size() - 1)));
    }
}
