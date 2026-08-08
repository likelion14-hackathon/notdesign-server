package com.likelionknu.notdesign.report.data.repository;

import com.likelionknu.notdesign.report.data.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @EntityGraph(attributePaths = {"plan", "result", "result.user"})
    Optional<Report> findWithPlanAndResultById(Long id);

    @EntityGraph(attributePaths = {"plan", "result", "result.user"})
    Optional<Report> findFirstByResult_User_IdOrderByCreatedAtDesc(Long userId);
}
