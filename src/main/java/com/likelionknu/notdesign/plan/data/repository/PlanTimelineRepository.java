package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanTimelineRepository extends JpaRepository<PlanTimeline, Long> {
    @EntityGraph(attributePaths = "item")
    List<PlanTimeline> findAllByPlan_IdOrderByItem_IdAsc(Long planId);

    void deleteByPlan_Id(Long planId);
}
