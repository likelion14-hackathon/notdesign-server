package com.likelionknu.notdesign.user.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.user.data.dto.response.UserResponseDto;
import com.likelionknu.notdesign.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자(회원) 관련 API 엔드포인트.
 * 인증된 사용자 본인에 대한 조회/수정을 제공한다.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 측정 동의.
     * 현재 로그인한 사용자의 측정 동의 시각을 기록한다.
     *
     * @return 갱신된 사용자 정보
     */
    @PatchMapping("/me")
    @Operation(summary = "측정 동의")
    public GlobalResponse<UserResponseDto> agreeMeasurement() {
        return userService.agreeMeasurement(SecurityUtil.getUsername());
    }
}
