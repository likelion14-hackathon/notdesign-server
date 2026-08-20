package com.likelionknu.notdesign.user.service;

import com.likelionknu.notdesign.user.data.dto.response.UserResponseDto;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    /**
     * 현재 로그인 사용자의 측정 동의 시각을 기록합니다.
     *
     * @param email 조회 대상 사용자 이메일
     * @return 갱신된 사용자 정보
     */
    @Transactional
    public UserResponseDto agreeMeasurement(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[agreeMeasurement] 사용자 조회 실패: {}", email);
                    return new UserNotFoundException();
                });

        user.agreeMeasurement();
        log.info("[agreeMeasurement] 측정 동의 완료: {}", email);

        return UserResponseDto.from(user);
    }
}
