package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.PlanItemEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanItemEffectRepository extends JpaRepository<PlanItemEffect, Long> {
    List<PlanItemEffect> findAllByNameIn(List<String> names);
}
