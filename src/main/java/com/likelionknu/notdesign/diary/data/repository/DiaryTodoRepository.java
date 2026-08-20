package com.likelionknu.notdesign.diary.data.repository;

import com.likelionknu.notdesign.diary.data.entity.DiaryTodo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiaryTodoRepository extends JpaRepository<DiaryTodo, Long> {
    @EntityGraph(attributePaths = {"checklist", "checklist.timeline", "checklist.timeline.item"})
    List<DiaryTodo> findAllByDiary_IdOrderByChecklist_Timeline_Item_IdAsc(Long diaryId);

    List<DiaryTodo> findAllByChecklist_IdIn(List<Long> checklistIds);
}
