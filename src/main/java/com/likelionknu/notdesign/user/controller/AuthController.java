package com.likelionknu.notdesign.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.user.data.dto.request.KakaoLoginRequestDto;
import com.likelionknu.notdesign.user.data.dto.request.SignInRequestDto;
import com.likelionknu.notdesign.user.data.dto.request.SignUpRequestDto;
import com.likelionknu.notdesign.user.data.dto.response.TokenResponseDto;
import com.likelionknu.notdesign.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API 엔드포인트.
 * 이메일 회원가입/로그인, 카카오 소셜 로그인, 로그아웃을 제공한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 이메일 회원가입.
     *
     * @param signUpRequestDto 이메일/이름/비밀번호
     * @return 발급된 액세스/리프레시 토큰
     */
    @PostMapping("/login/signup")
    @Operation(summary = "사용자 회원가입")
    public GlobalResponse<TokenResponseDto> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        return authService.signUp(signUpRequestDto);
    }

    /**
     * 이메일 로그인.
     *
     * @param signInRequestDto 이메일/비밀번호
     * @return 발급된 액세스/리프레시 토큰
     */
    @PostMapping("/login/signin")
    @Operation(summary = "사용자 로그인")
    public GlobalResponse<TokenResponseDto> signIn(@Valid @RequestBody SignInRequestDto signInRequestDto) {
        return authService.signIn(signInRequestDto);
    }

    /**
     * 카카오 소셜 로그인.
     * 프론트에서 받은 인가 코드로 카카오 토큰을 교환하고 사용자 정보를 조회한 뒤,
     * 가입 이력이 없으면 회원으로 등록하고 서비스 토큰을 발급한다.
     *
     * @param kakaoLoginRequestDto 카카오 인가 코드
     * @return 발급된 서비스 액세스/리프레시 토큰
     */
    @PostMapping("/login/kakao")
    @Operation(summary = "카카오 소셜 로그인")
    public GlobalResponse<TokenResponseDto> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequestDto kakaoLoginRequestDto) {
        return authService.kakaoLogin(kakaoLoginRequestDto);
    }

    /**
     * 로그아웃.
     * 리프레시 토큰을 삭제하고 현재 액세스 토큰을 만료 시점까지 블랙리스트에 등록한다.
     *
     * @param bearerToken Authorization 헤더의 Bearer 액세스 토큰
     * @return 빈 성공 응답
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public GlobalResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {
        return authService.logout(SecurityUtil.getUsername(), bearerToken);
    }
}
