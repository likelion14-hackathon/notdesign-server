package com.likelionknu.notdesign.diary.service;

import com.likelionknu.notdesign.diary.data.dto.request.DiaryCreateRequestDto;
import com.likelionknu.notdesign.diary.data.entity.DailyChecklist;
import com.likelionknu.notdesign.diary.data.entity.Diary;
import com.likelionknu.notdesign.diary.data.entity.DiaryTodo;
import com.likelionknu.notdesign.diary.data.repository.DailyChecklistRepository;
import com.likelionknu.notdesign.diary.data.repository.DiaryRepository;
import com.likelionknu.notdesign.diary.data.repository.DiaryTodoRepository;
import com.likelionknu.notdesign.diary.exception.DiaryDuplicateException;
import com.likelionknu.notdesign.plan.data.entity.PlanProcess;
import com.likelionknu.notdesign.plan.data.repository.PlanProcessRepository;
import com.likelionknu.notdesign.plan.exception.PlanProcessNotFoundException;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryRecordService {
    private final DiaryRepository diaryRepository;
    private final DiaryTodoRepository diaryTodoRepository;
    private final UserRepository userRepository;
    private final DailyChecklistRepository dailyChecklistRepository;
    private final PlanProcessRepository planProcessRepository;

    private void checkAlreadyRecorded(Long userId) {
        LocalDate today = LocalDate.now();

        boolean alreadyRecorded = !diaryRepository.findAllByProcess_User_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
                userId,
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX)
        ).isEmpty();

        if (alreadyRecorded) {
            throw new DiaryDuplicateException(userId, today);
        }
    }

    private void recordCheckList(Diary diary, Long processId, List<Long> doneChecklistIds) {
        // AI가 만들어 둔 체크리스트
        List<DailyChecklist> dailyChecklistList = dailyChecklistRepository
                .findAllByProcess_IdAndTargetDateOrderByTimeline_Item_IdAsc(processId, LocalDate.now());

        // 사용자가 체크한 항목
        if (doneChecklistIds == null) {
            doneChecklistIds = new ArrayList<>();
        }

        // 항목별 실천 여부
        List<DiaryTodo> diaryTodoList = new ArrayList<>();

        for (DailyChecklist dailyChecklist : dailyChecklistList) {
            diaryTodoList.add(DiaryTodo.builder()
                    .diary(diary)
                    .checklist(dailyChecklist)
                    .done(doneChecklistIds.contains(dailyChecklist.getId()))
                    .build());
        }

        diaryTodoRepository.saveAll(diaryTodoList);
    }

    /**
     * 사용자가 오늘 하루 기록을 남깁니다.
     *
     * @param email 기록을 남기는 사용자
     * @param diaryCreateRequestDto 슬라이더 3종, 사진, 한 줄 기록, 실천한 항목
     */
    @Transactional
    public void createDiary(String email, DiaryCreateRequestDto diaryCreateRequestDto) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        // 현재 진행 중인 플랜 조회
        PlanProcess planProcess = planProcessRepository.findByUser_IdAndCompletedAtIsNull(user.getId())
                .orElseThrow(() -> new PlanProcessNotFoundException(user.getId()));

        checkAlreadyRecorded(user.getId());

        Diary diary = Diary.builder()
                .process(planProcess)
                .skinTone(diaryCreateRequestDto.getSkinTone())
                .tightnessAndDryness(diaryCreateRequestDto.getTightnessAndDryness())
                .flushing(diaryCreateRequestDto.getFlushing())
                .comment(diaryCreateRequestDto.getComment())
                .recordedAt(LocalDateTime.now())
                .build();

        diaryRepository.save(diary);

        recordCheckList(diary, planProcess.getId(), diaryCreateRequestDto.getDoneChecklistIds());

        log.info("[createDiary] 기록 생성: userId={}, diaryId={}", user.getId(), diary.getId());
    }
}
