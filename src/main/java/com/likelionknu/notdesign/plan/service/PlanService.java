package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.response.PlanSummaryResponseDto;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.repository.PlanProcessRepository;
import com.likelionknu.notdesign.plan.exception.PlanProcessNotFoundException;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DAYS_PER_WEEK = 7;
    private static final int MID_REPORT_WEEK = 6;

    private final UserRepository userRepository;
    private final PlanProcessRepository planProcessRepository;

    /**
     * 홈 화면에서 현재 진행 중인 플랜의 진행 요약을 조회합니다.
     *
     * @param email 조회 대상 사용자
     * @return 진행 주차, 진행률, 중간/최종 리포트까지 남은 기간
     */
    @Transactional(readOnly = true)
    public PlanSummaryResponseDto getCurrentPlanSummary(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        PlanProcess process = planProcessRepository.findByUser_IdAndCompletedAtIsNull(user.getId())
                .orElseThrow(() -> new PlanProcessNotFoundException(user.getId()));

        Plan plan = process.getPlan();
        int totalWeeks = plan.getDurationWeeks();
        long totalDays = (long) totalWeeks * DAYS_PER_WEEK;

        LocalDate today = LocalDate.now(KST);
        LocalDate startedAt = process.getStartedAt();

        // 시작 전이면 0일, 전체 기간을 넘겼으면 전체 일수로 고정
        long elapsedDays = Math.max(0, Math.min(ChronoUnit.DAYS.between(startedAt, today), totalDays));

        int currentWeek = Math.min((int) (elapsedDays / DAYS_PER_WEEK) + 1, totalWeeks);
        int progressRate = (int) Math.round(elapsedDays * 100.0 / totalDays);

        // 중간=시작+6주, 최종=시작+12주 시점 (report/result 측정일 더미데이터로 검증)
        int daysToMidReport = daysUntil(today, startedAt.plusWeeks(MID_REPORT_WEEK));
        int daysToFinalReport = daysUntil(today, startedAt.plusWeeks(totalWeeks));

        return PlanSummaryResponseDto.builder()
                .currentWeek(currentWeek)
                .totalWeeks(totalWeeks)
                .progressRate(progressRate)
                .daysToMidReport(daysToMidReport)
                .daysToFinalReport(daysToFinalReport)
                .build();
    }

    private int daysUntil(LocalDate today, LocalDate target) {
        return (int) Math.max(0, ChronoUnit.DAYS.between(today, target));
    }
}
