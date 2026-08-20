package com.likelionknu.notdesign.diagnosis.data.repository;

import com.likelionknu.notdesign.diagnosis.data.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
}
