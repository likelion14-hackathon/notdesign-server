package com.likelionknu.notdesign.user.service;

import com.likelionknu.notdesign.common.redis.RedisService;
import com.likelionknu.notdesign.common.security.AuthenticationToken;
import com.likelionknu.notdesign.common.security.JwtTokenProvider;
import com.likelionknu.notdesign.user.data.dto.kakao.KakaoUserInfo;
import com.likelionknu.notdesign.user.data.dto.request.KakaoLoginRequestDto;
import com.likelionknu.notdesign.user.data.dto.request.SignInRequestDto;
import com.likelionknu.notdesign.user.data.dto.request.SignUpRequestDto;
import com.likelionknu.notdesign.user.data.dto.response.TokenResponseDto;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.enums.SocialProvider;
import com.likelionknu.notdesign.user.data.exception.EmailDuplicationException;
import com.likelionknu.notdesign.user.data.exception.PasswordInvalidException;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final RedisService redisService;

    @Transactional(readOnly = true)
    public User getUserEntity(String email) {
        log.info("[getUserEntity] 사용자 조회 시도: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[getUserEntity] 사용자 조회 실패: {}", email);
                    return new UserNotFoundException();
                });
    }

    private void validatePassword(String originalPassword, String password) {
        if(!passwordEncoder.matches(originalPassword, password)) {
            throw new PasswordInvalidException();
        }
    }

    private Authentication createAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().getValue())
        );

        return new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
    }

    @Transactional
    public TokenResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        User user = User.builder()
                .email(signUpRequestDto.getEmail())
                .name(signUpRequestDto.getName())
                .password(passwordEncoder.encode(signUpRequestDto.getPassword()))
                .build();

        try {
            userRepository.save(user);
            log.info("[signUp] 새로운 사용자 등록: {}", user.getEmail());
        } catch (DataIntegrityViolationException e) {
            log.error("[signUp] 중복된 이메일 주소로 인한 가입 거부: {}", signUpRequestDto.getEmail());
            throw new EmailDuplicationException();
        }

        Authentication authentication = createAuthentication(user);
        AuthenticationToken authenticationToken = tokenProvider.generateToken(authentication);

        return TokenResponseDto.builder()
                .accessToken(authenticationToken.getAccessToken())
                .refreshToken(authenticationToken.getRefreshToken())
                .build();
    }

    public TokenResponseDto signIn(SignInRequestDto signInRequestDto) {
        User user = getUserEntity(signInRequestDto.getEmail());
        validatePassword(signInRequestDto.getPassword(), user.getPassword());

        Authentication authentication = createAuthentication(user);
        AuthenticationToken authenticationToken = tokenProvider.generateToken(authentication);

        return TokenResponseDto.builder()
                .accessToken(authenticationToken.getAccessToken())
                .refreshToken(authenticationToken.getRefreshToken())
                .build();
    }

    @Transactional
    public TokenResponseDto kakaoLogin(KakaoLoginRequestDto kakaoLoginRequestDto) {
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoLoginRequestDto.getCode());

        User user = userRepository
                .findByProviderAndProviderId(SocialProvider.KAKAO, String.valueOf(userInfo.id()))
                .map(existing -> updateKakaoUser(existing, userInfo))
                .orElseGet(() -> registerKakaoUser(userInfo));

        Authentication authentication = createAuthentication(user);
        AuthenticationToken authenticationToken = tokenProvider.generateToken(authentication);

        return TokenResponseDto.builder()
                .accessToken(authenticationToken.getAccessToken())
                .refreshToken(authenticationToken.getRefreshToken())
                .build();
    }

    private User registerKakaoUser(KakaoUserInfo userInfo) {
        String email = (userInfo.email() != null && !userInfo.email().isBlank())
                ? userInfo.email()
                : "kakao_" + userInfo.id() + "@social.local";
        String name = (userInfo.nickname() != null && !userInfo.nickname().isBlank())
                ? userInfo.nickname()
                : "카카오사용자";

        User user = User.builder()
                .email(email)
                .name(name)
                .provider(SocialProvider.KAKAO)
                .providerId(String.valueOf(userInfo.id()))
                .build();

        log.info("[registerKakaoUser] 카카오 신규 회원 등록: providerId={}", userInfo.id());
        return userRepository.save(user);
    }

    private User updateKakaoUser(User user, KakaoUserInfo userInfo) {
        if (userInfo.nickname() != null && !userInfo.nickname().isBlank()) {
            user.updateProfile(userInfo.nickname());
            log.info("[updateKakaoUser] 카카오 회원 프로필 갱신: providerId={}", userInfo.id());
        }
        return user;
    }

    @Transactional
    public void logout(String email, String bearerToken) {
        String accessToken = tokenProvider.resolveBearer(bearerToken);

        redisService.deleteValues(email);
        tokenProvider.blacklistAccessToken(accessToken);

        log.info("[logout] 로그아웃 처리 완료: {}", email);
    }
}
