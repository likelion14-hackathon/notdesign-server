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

@Slf4j
@Component
public class KakaoOAuthClient {
    private final RestClient restClient = RestClient.create();

    @Value("${kakao.client-id:}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri:}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    /** 인가 코드로 토큰 교환 후 사용자 정보 조회 */
    public KakaoUserInfo getUserInfo(String code) {
        String kakaoAccessToken = requestAccessToken(code);
        return requestUserInfo(kakaoAccessToken);
    }

    private String requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        body.add("client_secret", clientSecret);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new SocialLoginException();
            }
            return response.accessToken();
        } catch (RestClientException e) {
            log.error("[requestAccessToken] 카카오 토큰 발급 실패: {}", e.getMessage());
            throw new SocialLoginException();
        }
    }

    private KakaoUserInfo requestUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new SocialLoginException();
            }

            KakaoUserResponse.KakaoAccount account = response.kakaoAccount();
            String email = (account != null) ? account.email() : null;
            String nickname = (account != null && account.profile() != null)
                    ? account.profile().nickname()
                    : null;

            return new KakaoUserInfo(response.id(), email, nickname);
        } catch (RestClientException e) {
            log.error("[requestUserInfo] 카카오 사용자 조회 실패: {}", e.getMessage());
            throw new SocialLoginException();
        }
    }
}
