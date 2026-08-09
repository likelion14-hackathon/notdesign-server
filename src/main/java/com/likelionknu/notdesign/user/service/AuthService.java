package com.likelionknu.notdesign.user.service;

import com.likelionknu.notdesign.common.redis.RedisService;
import com.likelionknu.notdesign.common.response.GlobalResponse;
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
    public GlobalResponse<TokenResponseDto> signUp(SignUpRequestDto signUpRequestDto) {
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

        return GlobalResponse.ok(
                TokenResponseDto.builder()
                        .accessToken(authenticationToken.getAccessToken())
                        .refreshToken(authenticationToken.getRefreshToken())
                        .build());
    }

    public GlobalResponse<TokenResponseDto> signIn(SignInRequestDto signInRequestDto) {
        User user = getUserEntity(signInRequestDto.getEmail());
        validatePassword(signInRequestDto.getPassword(), user.getPassword());

        Authentication authentication = createAuthentication(user);
        AuthenticationToken authenticationToken = tokenProvider.generateToken(authentication);

        return GlobalResponse.ok(
                TokenResponseDto.builder()
                        .accessToken(authenticationToken.getAccessToken())
                        .refreshToken(authenticationToken.getRefreshToken())
                        .build());
    }

    /**
     * 카카오 소셜 로그인.
     * 인가 코드로 카카오 사용자 정보를 조회하고, provider/providerId 로 기존 회원을 찾는다.
     * 가입 이력이 없으면 신규 회원으로 등록한 뒤 서비스 토큰을 발급한다.
     *
     * @param kakaoLoginRequestDto 카카오 인가 코드
     * @return 발급된 서비스 액세스/리프레시 토큰
     */
    @Transactional
    public GlobalResponse<TokenResponseDto> kakaoLogin(KakaoLoginRequestDto kakaoLoginRequestDto) {
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoLoginRequestDto.getCode());

        User user = userRepository
                .findByProviderAndProviderId(SocialProvider.KAKAO, String.valueOf(userInfo.getId()))
                .map(existing -> updateKakaoUser(existing, userInfo))
                .orElseGet(() -> registerKakaoUser(userInfo));

        Authentication authentication = createAuthentication(user);
        AuthenticationToken authenticationToken = tokenProvider.generateToken(authentication);

        return GlobalResponse.ok(
                TokenResponseDto.builder()
                        .accessToken(authenticationToken.getAccessToken())
                        .refreshToken(authenticationToken.getRefreshToken())
                        .build());
    }

    /**
     * 카카오 신규 회원을 등록한다.
     * 이메일/닉네임 동의를 받지 못한 경우를 대비해 대체 값을 생성한다.
     *
     * @param userInfo 카카오에서 조회한 사용자 정보
     * @return 저장된 회원 엔티티
     */
    private User registerKakaoUser(KakaoUserInfo userInfo) {
        String email = (userInfo.getEmail() != null && !userInfo.getEmail().isBlank())
                ? userInfo.getEmail()
                : "kakao_" + userInfo.getId() + "@social.local";
        String name = (userInfo.getNickname() != null && !userInfo.getNickname().isBlank())
                ? userInfo.getNickname()
                : "카카오사용자";

        User user = User.builder()
                .email(email)
                .name(name)
                .provider(SocialProvider.KAKAO)
                .providerId(String.valueOf(userInfo.getId()))
                .build();

        log.info("[registerKakaoUser] 카카오 신규 회원 등록: providerId={}", userInfo.getId());
        return userRepository.save(user);
    }

    /**
     * 기존 카카오 회원의 프로필을 최신 정보로 갱신한다.
     * 닉네임은 카카오에서 변경될 수 있으므로 로그인마다 최신값으로 맞춘다.
     * 닉네임 동의를 받지 못해 값이 없으면 기존 닉네임을 유지한다.
     *
     * @param user     기존 회원 엔티티
     * @param userInfo 카카오에서 조회한 최신 사용자 정보
     * @return 갱신된 회원 엔티티
     */
    private User updateKakaoUser(User user, KakaoUserInfo userInfo) {
        if (userInfo.getNickname() != null && !userInfo.getNickname().isBlank()) {
            user.updateProfile(userInfo.getNickname());
            log.info("[updateKakaoUser] 카카오 회원 프로필 갱신: providerId={}", userInfo.getId());
        }
        return user;
    }

    /**
     * 로그아웃.
     * 저장된 리프레시 토큰을 삭제해 재발급을 막고, 현재 액세스 토큰을 남은 만료 시간만큼
     * 블랙리스트에 등록해 재사용을 차단한다.
     *
     * @param email       현재 인증된 사용자 이메일
     * @param bearerToken Authorization 헤더의 Bearer 액세스 토큰
     * @return 빈 성공 응답
     */
    @Transactional
    public GlobalResponse<Void> logout(String email, String bearerToken) {
        String accessToken = tokenProvider.resolveBearer(bearerToken);

        redisService.deleteValues(email);
        tokenProvider.blacklistAccessToken(accessToken);

        log.info("[logout] 로그아웃 처리 완료: {}", email);
        return GlobalResponse.ok();
    }
}
