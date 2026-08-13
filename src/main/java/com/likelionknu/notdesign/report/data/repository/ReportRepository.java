package com.likelionknu.notdesign.report.data.repository;

import com.likelionknu.notdesign.report.data.entity.Report;
import com.likelionknu.notdesign.report.data.enums.ReportType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @EntityGraph(attributePaths = {"plan", "result", "result.user"})
    Optional<Report> findWithPlanAndResultById(Long id);

    @EntityGraph(attributePaths = {"plan", "result", "result.user"})
    Optional<Report> findFirstByResult_User_IdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"plan", "result", "result.user"})
    Optional<Report> findFirstByResult_User_IdAndTypeOrderByCreatedAtDesc(Long userId, ReportType type);

    boolean existsByPlan_IdInAndType(List<Long> planIds, ReportType type);

    boolean existsByResult_Id(Long resultId);
}
