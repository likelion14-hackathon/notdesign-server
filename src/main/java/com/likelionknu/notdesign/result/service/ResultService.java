package com.likelionknu.notdesign.result.service;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import com.likelionknu.notdesign.clinic.data.exception.ClinicNotFoundException;
import com.likelionknu.notdesign.clinic.data.repository.ClinicRepository;
import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.result.data.dto.request.ResultCreateRequestDto;
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

/**
 * 측정 결과 도메인 서비스.
 * 클리닉 방문 측정을 시뮬레이션하기 위해 더미 측정값(result_dummy)을 실제 결과(result)로 복사하고,
 * 사용자 본인의 결과를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultService {
    private final ResultRepository resultRepository;
    private final ResultDummyRepository resultDummyRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;

    /**
     * 측정 결과를 생성한다.
     * 요청한 클리닉에 대해 현재 사용자의 최신 더미 측정값을 찾아 실제 결과로 복사한다.
     *
     * @param email   현재 로그인 사용자 이메일(식별자)
     * @param request 측정 클리닉 정보
     * @return 생성된 측정 결과
     */
    @Transactional
    public GlobalResponse<ResultResponseDto> createResult(String email, ResultCreateRequestDto request) {
        User user = getUser(email);

        ResultDummy dummy = resultDummyRepository
                .findFirstByUserOrderByMeasuredAtDesc(user)
                .orElseThrow(() -> {
                    log.error("[createResult] 더미 측정값 없음: userId={}", user.getId());
                    return new ResultNotFoundException();
                });

        Clinic clinic = resolveClinic(request, dummy);

        Result result = Result.builder()
                .user(user)
                .clinic(clinic)
                .pigmentation(dummy.getPigmentation())
                .erythema(dummy.getErythema())
                .hydration(dummy.getHydration())
                .measuredAt(dummy.getMeasuredAt())
                .build();

        Result saved = resultRepository.save(result);
        log.info("[createResult] 측정 결과 생성: resultId={}, userId={}", saved.getId(), user.getId());

        return GlobalResponse.ok(ResultResponseDto.from(saved));
    }

    /**
     * 현재 사용자의 전체 측정 결과를 최신순으로 조회한다.
     *
     * @param email 현재 로그인 사용자 이메일(식별자)
     * @return 측정 결과 목록
     */
    @Transactional(readOnly = true)
    public GlobalResponse<List<ResultResponseDto>> getResults(String email) {
        User user = getUser(email);
        List<ResultResponseDto> results = resultRepository.findAllByUserOrderByMeasuredAtDesc(user).stream()
                .map(ResultResponseDto::from)
                .toList();

        return GlobalResponse.ok(results);
    }

    /**
     * 측정 결과 단건을 조회한다. 본인 소유가 아니면 조회할 수 없다.
     *
     * @param email    현재 로그인 사용자 이메일(식별자)
     * @param resultId 조회할 측정 결과 ID
     * @return 측정 결과 상세
     */
    @Transactional(readOnly = true)
    public GlobalResponse<ResultResponseDto> getResult(String email, Long resultId) {
        User user = getUser(email);
        Result result = resultRepository.findById(resultId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(ResultNotFoundException::new);

        return GlobalResponse.ok(ResultResponseDto.from(result));
    }

    /**
     * 생성될 결과에 연결할 클리닉을 결정한다.
     * 요청에 clinicId 가 있으면 해당 클리닉을, 없으면 더미에 기록된 클리닉(없을 수 있음)을 사용한다.
     *
     * @param request 결과 생성 요청(선택적 clinicId)
     * @param dummy   복사 대상 더미 측정값
     * @return 연결할 클리닉(없으면 null)
     */
    private Clinic resolveClinic(ResultCreateRequestDto request, ResultDummy dummy) {
        if (request != null && request.getClinicId() != null) {
            return clinicRepository.findById(request.getClinicId())
                    .orElseThrow(() -> {
                        log.error("[createResult] 클리닉 조회 실패: clinicId={}", request.getClinicId());
                        return new ClinicNotFoundException();
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
