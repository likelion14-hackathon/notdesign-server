package com.likelionknu.notdesign.result.data.repository;

import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findAllByUserOrderByMeasuredAtDesc(User user);
}
