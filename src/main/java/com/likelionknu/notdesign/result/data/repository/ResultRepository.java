package com.likelionknu.notdesign.result.data.repository;

import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findAllByUserOrderByMeasuredAtDesc(User user);
    Optional<Result> findFirstByPlan_IdOrderByMeasuredAtAsc(Long planId);

    Optional<Result> findFirstByUser_IdOrderByMeasuredAtDesc(Long userId);

    Optional<Result> findFirstByUser_IdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
            Long userId, LocalDateTime startedAt);

    Optional<Result> findFirstByUser_IdAndMeasuredAtLessThanOrderByMeasuredAtDesc(
            Long userId, LocalDateTime startedAt);
}
