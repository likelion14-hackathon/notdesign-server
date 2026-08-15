package com.likelionknu.notdesign.diagnosis.data.entity;

import com.likelionknu.notdesign.diagnosis.data.enums.Attribution;
import com.likelionknu.notdesign.diagnosis.data.enums.FeltEffect;
import com.likelionknu.notdesign.diagnosis.data.enums.UnderstandingGrade;
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
@Table(name = "diagnosis")
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "procedure_cost", nullable = false)
    private Integer procedureCost;

    @Column(name = "product_cost", nullable = false)
    private Integer productCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeltEffect feltEffect;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Attribution attribution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnderstandingGrade grade;

    @Column(name = "waste_rate", nullable = false)
    private Integer wasteRate;

    @Column(name = "waste_amount", nullable = false)
    private Integer wasteAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
