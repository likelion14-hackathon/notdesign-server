package com.likelionknu.notdesign.report.data.entity;

import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import com.likelionknu.notdesign.report.data.enums.Reliability;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "report_items")
public class ReportItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImprovementItem improvement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeline_id", nullable = false)
    private PlanTimeline timeline;

    @Column
    private BigDecimal score;

    @Column(name = "contribution_rate")
    private BigDecimal contributionRate;

    @Column(name = "cost_per_point")
    private Integer costPerPoint;

    @Column
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column
    private Reliability reliability;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
