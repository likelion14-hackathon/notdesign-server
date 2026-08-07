package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.PlanItemEffect;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanItemRepository extends JpaRepository<PlanItemEffect, Long> {
}
