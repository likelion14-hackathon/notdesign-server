package com.likelionknu.notdesign.result.service;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import com.likelionknu.notdesign.clinic.data.repository.ClinicRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultService {
    private final ResultRepository resultRepository;
    private final ResultDummyRepository resultDummyRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResultResponseDto createResult(String email, ResultCreateRequestDto request) {
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

        return ResultResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ResultResponseDto> getResults(String email) {
        User user = getUser(email);
        return resultRepository.findAllByUserOrderByMeasuredAtDesc(user).stream()
                .map(ResultResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultResponseDto getResult(String email, Long resultId) {
        User user = getUser(email);
        Result result = resultRepository.findById(resultId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(ResultNotFoundException::new);

        return ResultResponseDto.from(result);
    }

    private Clinic resolveClinic(ResultCreateRequestDto request, ResultDummy dummy) {
        if (request != null && request.getClinicId() != null) {
            return clinicRepository.findById(request.getClinicId())
                    .orElseThrow(() -> {
                        log.error("[createResult] 클리닉 조회 실패: clinicId={}", request.getClinicId());
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
