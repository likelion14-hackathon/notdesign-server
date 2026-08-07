package com.likelionknu.notdesign.diary.data.repository;

import com.likelionknu.notdesign.diary.data.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
}
