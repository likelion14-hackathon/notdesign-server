package com.likelionknu.notdesign.user.data.dto.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 토큰 발급 API(kauth.kakao.com/oauth/token) 응답 매핑 DTO.
 * 필요한 필드만 매핑하고 그 외 필드는 무시한다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoTokenResponse {
    /** 카카오 API 호출에 사용할 액세스 토큰. */
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;
}
