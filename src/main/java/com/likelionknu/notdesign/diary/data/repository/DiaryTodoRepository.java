package com.likelionknu.notdesign.diary.data.repository;

import com.likelionknu.notdesign.diary.data.entity.DiaryTodo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiaryTodoRepository extends JpaRepository<DiaryTodo, Long> {
    @EntityGraph(attributePaths = {"timeline", "timeline.item"})
    List<DiaryTodo> findAllByDiary_IdOrderByTimeline_Item_IdAsc(Long diaryId);
}
