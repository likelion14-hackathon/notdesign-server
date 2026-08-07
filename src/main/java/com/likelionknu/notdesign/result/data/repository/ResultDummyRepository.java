package com.likelionknu.notdesign.result.data.repository;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import com.likelionknu.notdesign.result.data.entity.ResultDummy;
import com.likelionknu.notdesign.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultDummyRepository extends JpaRepository<ResultDummy, Long> {
    Optional<ResultDummy> findFirstByUserAndClinicOrderByMeasuredAtDesc(User user, Clinic clinic);
}
