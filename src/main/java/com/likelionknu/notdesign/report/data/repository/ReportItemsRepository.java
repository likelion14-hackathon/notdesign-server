package com.likelionknu.notdesign.report.data.repository;

import com.likelionknu.notdesign.report.data.entity.ReportItems;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportItemsRepository extends JpaRepository<ReportItems, Long> {
    @EntityGraph(attributePaths = {"timeline", "timeline.item"})
    List<ReportItems> findAllByReport_IdOrderByImprovementAscContributionRateDesc(Long reportId);
}
