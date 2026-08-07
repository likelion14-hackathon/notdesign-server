package com.likelionknu.notdesign.report.data.repository;

import com.likelionknu.notdesign.report.data.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
