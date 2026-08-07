package com.likelionknu.notdesign.diary.service;

import com.likelionknu.notdesign.diary.data.dto.response.DiaryResponseDto;
import com.likelionknu.notdesign.diary.data.dto.response.DiaryTodoResponseDto;
import com.likelionknu.notdesign.diary.data.entity.Diary;
import com.likelionknu.notdesign.diary.exception.DiaryNotFoundException;
import com.likelionknu.notdesign.diary.data.repository.DiaryRepository;
import com.likelionknu.notdesign.diary.data.repository.DiaryTodoRepository;
import com.likelionknu.notdesign.plan.data.entity.PlanItem;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final DiaryTodoRepository diaryTodoRepository;
    private final UserRepository userRepository;

    /**
     * 기록을 남기는 부분에서 특정 일자 기록 조회를 담당합니다.
     *
     * @param email 조회 대상이 되는 사용자
     * @param recordedDate 특정 일자(0000-00-00)
     * @return 사용자가 입력한 피부톤, 당김, 건조함 정도 등의 기록
     */
    @Transactional(readOnly = true)
    public DiaryResponseDto getDiary(String email, LocalDate recordedDate) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        Diary diary = diaryRepository.findAllByProcess_User_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
                user.getId(),
                recordedDate.atStartOfDay(),
                recordedDate.atTime(LocalTime.MAX)
        ).stream().findFirst()
                .orElseThrow(() -> new DiaryNotFoundException(user.getId(), recordedDate));

        List<DiaryTodoResponseDto> todos = diaryTodoRepository.findAllByDiary_IdOrderByTimeline_Item_IdAsc(diary.getId())
                .stream()
                .map(todo -> {
                    PlanItem item = todo.getTimeline().getItem();

                    return DiaryTodoResponseDto.builder()
                            .category(item.getCategory())
                            .categoryName(item.getCategory().getDisplayName())
                            .name(item.getName())
                            .done(todo.getDone())
                            .build();
                })
                .toList();

        return DiaryResponseDto.builder()
                .diaryId(diary.getId())
                .skinTone(diary.getSkinTone())
                .tightnessAndDryness(diary.getTightnessAndDryness())
                .flushing(diary.getFlushing())
                .comment(diary.getComment())
                .todos(todos)
                .build();
    }
}
