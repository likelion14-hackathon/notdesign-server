package com.likelionknu.notdesign.result.data.entity;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.user.data.entity.User;
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
@Table(name = "result")
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @Column
    private Integer pigmentation;

    @Column
    private Integer erythema;

    @Column
    private Integer hydration;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void linkPlan(Plan plan) {
        this.plan = plan;
    }
}
