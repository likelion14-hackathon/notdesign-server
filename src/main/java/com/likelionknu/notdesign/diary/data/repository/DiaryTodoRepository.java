package com.likelionknu.notdesign.diary.data.repository;

import com.likelionknu.notdesign.diary.data.entity.DiaryTodo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryTodoRepository extends JpaRepository<DiaryTodo, Long> {
}
