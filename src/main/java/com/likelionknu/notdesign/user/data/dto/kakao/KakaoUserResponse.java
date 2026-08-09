package com.likelionknu.notdesign.user.data.dto.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 조회 API(kapi.kakao.com/v2/user/me) 응답 매핑 DTO.
 * 카카오 응답의 중첩 구조(kakao_account.profile)를 그대로 반영한다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserResponse {
    /** 카카오 회원 고유 식별자. */
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    /** 카카오 계정 정보(이메일/프로필). */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    /** 카카오 프로필 정보(닉네임 등). */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String nickname;
    }
}
