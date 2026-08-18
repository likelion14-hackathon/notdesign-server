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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login/signup")
    @Operation(summary = "사용자 회원가입")
    public GlobalResponse<TokenResponseDto> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        return GlobalResponse.ok(authService.signUp(signUpRequestDto));
    }

    @PostMapping("/login/signin")
    @Operation(summary = "사용자 로그인")
    public GlobalResponse<TokenResponseDto> signIn(@Valid @RequestBody SignInRequestDto signInRequestDto) {
        return GlobalResponse.ok(authService.signIn(signInRequestDto));
    }

    @PostMapping("/login/kakao")
    @Operation(summary = "카카오 소셜 로그인")
    public GlobalResponse<TokenResponseDto> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequestDto kakaoLoginRequestDto) {
        return GlobalResponse.ok(authService.kakaoLogin(kakaoLoginRequestDto));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public GlobalResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {
        authService.logout(SecurityUtil.getUsername(), bearerToken);
        return GlobalResponse.ok();
    }
}
