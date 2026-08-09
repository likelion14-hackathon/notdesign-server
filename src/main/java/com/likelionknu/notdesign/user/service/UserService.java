package com.likelionknu.notdesign.user.service;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.user.data.dto.response.UserResponseDto;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자(회원) 관련 도메인 서비스.
 * 내 정보 조회/수정 등 인증된 사용자 본인에 대한 작업을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    /**
     * 현재 로그인한 사용자의 측정 동의를 처리한다.
     * {@code measurementAgreedAt} 을 현재 시각으로 기록한다.
     *
     * @param email 현재 로그인 사용자 이메일(식별자)
     * @return 갱신된 사용자 정보
     */
    @Transactional
    public GlobalResponse<UserResponseDto> agreeMeasurement(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[agreeMeasurement] 사용자 조회 실패: {}", email);
                    return new UserNotFoundException();
                });

        user.agreeMeasurement();
        log.info("[agreeMeasurement] 측정 동의 완료: {}", email);

        return GlobalResponse.ok(UserResponseDto.from(user));
    }
}
