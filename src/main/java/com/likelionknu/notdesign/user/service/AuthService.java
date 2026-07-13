package com.likelionknu.notdesign.user.service;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.AuthenticationToken;
import com.likelionknu.notdesign.common.security.JwtTokenProvider;
import com.likelionknu.notdesign.user.data.dto.request.SignInRequestDto;
import com.likelionknu.notdesign.user.data.dto.request.SignUpRequestDto;
import com.likelionknu.notdesign.user.data.dto.response.TokenResponseDto;
import com.likelionknu.notdesign.user.data.entity.User;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

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
                .registeredAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
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
}
