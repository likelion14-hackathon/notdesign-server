package com.likelionknu.notdesign.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import com.likelionknu.notdesign.common.redis.RedisService;
import com.likelionknu.notdesign.user.data.enums.UserRole;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final SecretKey key;
    private final RedisService redisService;
    private final UserRepository userRepository;

    // AT 만료 시간
    @Value("${jwt.access-token.expire-time}")
    private long ACCESS_TOKEN_EXPIRE_TIME;

    // RT 만료 시간
    @Value("${jwt.refresh-token.expire-time}")
    private long REFRESH_TOKEN_EXPIRE_TIME;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            RedisService redisService,
                            UserRepository userRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisService = redisService;
        this.userRepository = userRepository;
    }

    private String createToken(String email, String authorities, Date expireDate) {
        log.info("[createToken] 새 JWT 발급 됨: {}", email);
        JwtBuilder builder = Jwts.builder()
                .subject(email)
                .expiration(expireDate)
                .signWith(key, Jwts.SIG.HS256);

        if (authorities != null && !authorities.isEmpty()) {
            builder.claim("authorities", authorities);
        }

        return builder.compact();
    }

    public AuthenticationToken generateToken(Authentication authentication) {
        String username = authentication.getName();

        log.info("[generateToken] 새 JWT 발급 시도: {}", username);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        String accessToken = createToken(username, authorities, new Date(now + ACCESS_TOKEN_EXPIRE_TIME));
        String refreshToken = createToken(username, null, new Date(now + REFRESH_TOKEN_EXPIRE_TIME));

        log.info("[generateToken] 발급된 Refresh Token이 Redis에 저장 됨");
        redisService.setValues(username, refreshToken, Duration.ofMillis(REFRESH_TOKEN_EXPIRE_TIME));

        return AuthenticationToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        Object authClaim = claims.get("authorities");

        Collection<? extends GrantedAuthority> authorities;

        if (authClaim == null || authClaim.toString().isEmpty()) {
            String role = userRepository.findByEmail(claims.getSubject())
                    .map(user -> user.getRole().getValue())
                    .orElse(UserRole.USER.getValue());
            authorities = List.of(new SimpleGrantedAuthority(role));
        } else {
            authorities = Arrays.stream(authClaim.toString().split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        UserDetails principal = new org.springframework.security.core.userdetails.User(claims.getSubject(),
                "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("[validateToken] 유효하지 않은 JWT 서명 요청: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("[validateToken] 만료된 JWT 인증 요청: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[validateToken] 지원되지 않는 JWT 인증 요청: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[validateToken] JWT가 제출되지 않음: {}", e.getMessage());
        }
        return false;
    }

    /**
     * "Bearer " 접두사를 제거해 순수 토큰 문자열을 반환한다.
     *
     * @param bearerToken Authorization 헤더 값
     * @return 접두사를 제거한 토큰(접두사가 없으면 입력값 그대로)
     */
    public String resolveBearer(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return bearerToken;
    }

    /**
     * 로그아웃 시 액세스 토큰을 남은 만료 시간만큼 블랙리스트에 등록한다.
     *
     * @param accessToken 무효화할 액세스 토큰
     */
    public void blacklistAccessToken(String accessToken) {
        long remaining = getRemainingExpiration(accessToken);
        if (remaining > 0) {
            redisService.setValues(BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(remaining));
        }
    }

    /**
     * 액세스 토큰이 블랙리스트(로그아웃 처리)에 등록되어 있는지 확인한다.
     *
     * @param accessToken 검사할 액세스 토큰
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String accessToken) {
        return redisService.getValues(BLACKLIST_PREFIX + accessToken) != null;
    }

    /**
     * 토큰의 남은 만료 시간(밀리초)을 계산한다.
     *
     * @param accessToken 대상 토큰
     * @return 남은 시간(ms). 이미 만료되었으면 0 이하
     */
    private long getRemainingExpiration(String accessToken) {
        Date expiration = parseClaims(accessToken).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}
