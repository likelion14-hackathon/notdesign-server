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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[getUserEntity] 사용자 조회 실패: {}", email);
                    return new UserNotFoundException();
                });
    }

    /** 이메일 회원가입 후 서비스 토큰 발급 */
    @Transactional
    public TokenResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        User user = User.builder()
                .email(signUpRequestDto.getEmail())
                .name(signUpRequestDto.getName())
                .password(passwordEncoder.encode(signUpRequestDto.getPassword()))
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.error("[signUp] 중복된 이메일 주소로 인한 가입 거부: {}", signUpRequestDto.getEmail());
            throw new EmailDuplicationException();
        }

        return issueToken(user);
    }

    /** 이메일/비밀번호 검증 후 서비스 토큰 발급 */
    public TokenResponseDto signIn(SignInRequestDto signInRequestDto) {
        User user = getUserEntity(signInRequestDto.getEmail());
        validatePassword(signInRequestDto.getPassword(), user.getPassword());
        return issueToken(user);
    }

    /** 카카오 인가 코드로 로그인 (기존 회원은 프로필 갱신, 없으면 신규 가입) */
    @Transactional
    public TokenResponseDto kakaoLogin(KakaoLoginRequestDto kakaoLoginRequestDto) {
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoLoginRequestDto.getCode());

        User user = userRepository
                .findByProviderAndProviderId(SocialProvider.KAKAO, String.valueOf(userInfo.id()))
                .map(existing -> updateKakaoUser(existing, userInfo))
                .orElseGet(() -> registerKakaoUser(userInfo));

        return issueToken(user);
    }

    /** 리프레시 토큰 삭제 + 액세스 토큰 블랙리스트 등록 */
    @Transactional
    public void logout(String email, String bearerToken) {
        String accessToken = tokenProvider.resolveBearer(bearerToken);

        redisService.deleteValues(email);
        tokenProvider.blacklistAccessToken(accessToken);
    }

    private User registerKakaoUser(KakaoUserInfo userInfo) {
        // 이메일/닉네임 동의를 못 받으면 대체값 사용
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

        return userRepository.save(user);
    }

    private User updateKakaoUser(User user, KakaoUserInfo userInfo) {
        // 닉네임은 변경될 수 있어 로그인마다 최신값으로 갱신 (미동의 시 기존 값 유지)
        if (userInfo.nickname() != null && !userInfo.nickname().isBlank()) {
            user.updateProfile(userInfo.nickname());
        }
        return user;
    }

    private TokenResponseDto issueToken(User user) {
        Authentication authentication = createAuthentication(user);
        AuthenticationToken token = tokenProvider.generateToken(authentication);
        return TokenResponseDto.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .build();
    }

    private void validatePassword(String originalPassword, String password) {
        if (!passwordEncoder.matches(originalPassword, password)) {
            throw new PasswordInvalidException();
        }
    }

    private Authentication createAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().getValue())
        );
        return new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
    }
}
