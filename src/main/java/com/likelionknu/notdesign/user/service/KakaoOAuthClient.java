package com.likelionknu.notdesign.user.service;

import com.likelionknu.notdesign.user.data.dto.kakao.KakaoTokenResponse;
import com.likelionknu.notdesign.user.data.dto.kakao.KakaoUserInfo;
import com.likelionknu.notdesign.user.data.dto.kakao.KakaoUserResponse;
import com.likelionknu.notdesign.user.data.exception.SocialLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 OAuth 연동 클라이언트.
 * 인가 코드로 카카오 토큰을 교환하고, 그 토큰으로 카카오 사용자 정보를 조회한다.
 *
 * <p>동작에 필요한 값은 설정 파일의 {@code kakao.*} 로 주입된다.
 * 실제 발급받은 REST API 키와 redirect-uri 를 채워야 정상 동작한다.
 */
@Slf4j
@Component
public class KakaoOAuthClient {
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    /** 카카오 REST API 키(client_id). 발급 후 설정에 채워야 한다. */
    @Value("${kakao.client-id:}")
    private String clientId;

    /** 카카오 client_secret. 사용하지 않으면 비워둔다. */
    @Value("${kakao.client-secret:}")
    private String clientSecret;

    /** 카카오 앱에 등록한 redirect-uri. 발급 후 설정에 채워야 한다. */
    @Value("${kakao.redirect-uri:}")
    private String redirectUri;

    /**
     * 인가 코드로 카카오 사용자 정보를 조회한다.
     *
     * @param code 프론트에서 전달받은 카카오 인가 코드
     * @return 카카오 사용자 식별자/이메일/닉네임
     */
    public KakaoUserInfo getUserInfo(String code) {
        String kakaoAccessToken = requestAccessToken(code);
        return requestUserInfo(kakaoAccessToken);
    }

    /**
     * 인가 코드를 카카오 액세스 토큰으로 교환한다.
     *
     * @param code 카카오 인가 코드
     * @return 카카오 액세스 토큰
     */
    private String requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            body.add("client_secret", clientSecret);
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new SocialLoginException();
            }
            return response.getAccessToken();
        } catch (RestClientException e) {
            log.error("[requestAccessToken] 카카오 토큰 발급 실패: {}", e.getMessage());
            throw new SocialLoginException();
        }
    }

    /**
     * 카카오 액세스 토큰으로 사용자 정보를 조회한다.
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @return 평탄화한 카카오 사용자 정보
     */
    private KakaoUserInfo requestUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.getId() == null) {
                throw new SocialLoginException();
            }

            KakaoUserResponse.KakaoAccount account = response.getKakaoAccount();
            String email = (account != null) ? account.getEmail() : null;
            String nickname = (account != null && account.getProfile() != null)
                    ? account.getProfile().getNickname()
                    : null;

            return KakaoUserInfo.builder()
                    .id(response.getId())
                    .email(email)
                    .nickname(nickname)
                    .build();
        } catch (RestClientException e) {
            log.error("[requestUserInfo] 카카오 사용자 조회 실패: {}", e.getMessage());
            throw new SocialLoginException();
        }
    }
}
