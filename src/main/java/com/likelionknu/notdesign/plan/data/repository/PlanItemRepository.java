package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {
}
