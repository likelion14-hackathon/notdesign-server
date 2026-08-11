package com.likelionknu.notdesign.plan.data.repository;

import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlanProcessRepository extends JpaRepository<PlanProcess, Long> {
    Optional<PlanProcess> findByUser_IdAndCompletedAtIsNull(Long userId);

    boolean existsByUser_IdAndCompletedAtIsNull(Long userId);
}
