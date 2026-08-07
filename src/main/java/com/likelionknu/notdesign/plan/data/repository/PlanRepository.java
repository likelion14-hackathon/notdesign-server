package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
}
