package com.likelionknu.notdesign.report.data.entity;

import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.report.data.enums.ReportType;
import com.likelionknu.notdesign.result.data.entity.Result;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "report")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false, unique = true)
    private Result result;

    @Column(length = 1000)
    private String summary;

    @Column(name = "next_plan_suggestion", length = 1000)
    private String nextPlanSuggestion;

    @Column(name = "next_plan_price")
    private Integer nextPlanPrice;

    @Column(name = "pigmentation_delta")
    private Integer pigmentationDelta;

    @Column(name = "erythema_delta")
    private Integer erythemaDelta;

    @Column(name = "pores_delta")
    private Integer poresDelta;

    /**
     * @deprecated 지표 변경으로 인한 삭제 예정
     */
    @Deprecated
    @Column(name = "hydration_delta")
    private Integer hydrationDelta;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
