package com.likelionknu.notdesign.diary.data.repository;

import com.likelionknu.notdesign.diary.data.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findAllByProcess_User_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    long countByProcess_Id(Long processId);

    List<Diary> findAllByProcess_IdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(
            Long processId, LocalDateTime measuredAt);
}
