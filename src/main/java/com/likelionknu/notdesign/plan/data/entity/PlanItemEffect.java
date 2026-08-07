package com.likelionknu.notdesign.plan.data.entity;

import com.likelionknu.notdesign.plan.data.enums.ImprovementItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "item_effect")
public class PlanItemEffect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImprovementItem improvement;

    @Column(nullable = false)
    private BigDecimal expectedWeight;
}