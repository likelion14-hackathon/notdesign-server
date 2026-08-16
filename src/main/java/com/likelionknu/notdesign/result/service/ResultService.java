package com.likelionknu.notdesign.result.service;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import com.likelionknu.notdesign.clinic.data.repository.ClinicRepository;
import com.likelionknu.notdesign.result.data.dto.response.ResultResponseDto;
import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.result.data.entity.ResultDummy;
import com.likelionknu.notdesign.result.data.exception.ResultNotFoundException;
import com.likelionknu.notdesign.result.data.repository.ResultDummyRepository;
import com.likelionknu.notdesign.result.data.repository.ResultRepository;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultService {
    private final ResultRepository resultRepository;
    private final ResultDummyRepository resultDummyRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;

    /**
     * 측정 결과를 생성합니다. 사용자의 최신 더미 측정값(result_dummy)을 실제 결과(result)로 복사합니다.
     *
     * @param email    조회 대상 사용자 이메일
     * @param clinicId 연결할 클리닉 ID(선택)
     * @return 생성된 측정 결과
     */
    @Transactional
    public ResultResponseDto createResult(String email, Long clinicId) {
        User user = getUser(email);

        // 클리닉 방문 측정을 시뮬레이션하기 위해 준비된 더미 측정값을 가져온다.
        ResultDummy dummy = resultDummyRepository
                .findFirstByUserOrderByMeasuredAtDesc(user)
                .orElseThrow(() -> {
                    log.error("[createResult] 더미 측정값 없음: userId={}", user.getId());
                    return new ResultNotFoundException();
                });

        Clinic clinic = resolveClinic(clinicId, dummy);

        Result result = Result.builder()
                .user(user)
                .clinic(clinic)
                .pigmentation(dummy.getPigmentation())
                .erythema(dummy.getErythema())
                .pores(dummy.getPores())
                .measuredAt(dummy.getMeasuredAt())
                .build();

        Result saved = resultRepository.save(result);
        log.info("[createResult] 측정 결과 생성: resultId={}, userId={}", saved.getId(), user.getId());

        return ResultResponseDto.from(saved);
    }

    /**
     * 현재 사용자의 측정 결과 목록을 최신순으로 조회합니다.
     *
     * @param email 조회 대상 사용자 이메일
     * @return 측정 결과 목록
     */
    @Transactional(readOnly = true)
    public List<ResultResponseDto> getResults(String email) {
        User user = getUser(email);
        return resultRepository.findAllByUserOrderByMeasuredAtDesc(user).stream()
                .map(ResultResponseDto::from)
                .toList();
    }

    /**
     * 측정 결과 단건을 조회합니다. 본인 소유가 아니면 조회할 수 없습니다.
     *
     * @param email    조회 대상 사용자 이메일
     * @param resultId 조회할 측정 결과 ID
     * @return 측정 결과 상세
     */
    @Transactional(readOnly = true)
    public ResultResponseDto getResult(String email, Long resultId) {
        User user = getUser(email);
        // 본인 소유가 아니면 없는 것으로 처리한다.
        Result result = resultRepository.findById(resultId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(ResultNotFoundException::new);

        return ResultResponseDto.from(result);
    }

    // clinicId가 있으면 해당 클리닉, 없으면 더미에 기록된 클리닉(없을 수 있음)을 사용
    private Clinic resolveClinic(Long clinicId, ResultDummy dummy) {
        if (clinicId != null) {
            return clinicRepository.findById(clinicId)
                    .orElseThrow(() -> {
                        log.error("[createResult] 클리닉 조회 실패: clinicId={}", clinicId);
                        return new ResultNotFoundException();
                    });
        }
        return dummy.getClinic();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[ResultService] 사용자 조회 실패: {}", email);
                    return new UserNotFoundException();
                });
    }
}
