package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.PlanTimelineWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanTimelineWeekRepository extends JpaRepository<PlanTimelineWeek, Long> {
    List<PlanTimelineWeek> findAllByTimeline_IdInOrderByWeekAsc(List<Long> timelineIds);
}
