package com.likelionknu.notdesign.diary.data.repository;

import com.likelionknu.notdesign.diary.data.entity.DailyChecklist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyChecklistRepository extends JpaRepository<DailyChecklist, Long> {
    @EntityGraph(attributePaths = {"timeline", "timeline.item"})
    List<DailyChecklist> findAllByProcess_IdAndTargetDateOrderByTimeline_Item_IdAsc(Long processId, LocalDate targetDate);

    @EntityGraph(attributePaths = {"timeline", "timeline.item"})
    List<DailyChecklist> findAllByProcess_IdAndTargetDateLessThanEqual(Long processId, LocalDate targetDate);

    void deleteByProcess_IdAndTargetDateGreaterThan(Long processId, LocalDate targetDate);

    boolean existsByProcess_Id(Long processId);
}
