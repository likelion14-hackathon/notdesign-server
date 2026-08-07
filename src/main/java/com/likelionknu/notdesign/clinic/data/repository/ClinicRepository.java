package com.likelionknu.notdesign.clinic.data.repository;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
}
