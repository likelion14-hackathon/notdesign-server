package com.likelionknu.notdesign.user.data.dto.kakao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 카카오에서 조회한 사용자 정보를 서비스 내부에서 사용하기 위한 홀더.
 * 카카오 응답 구조(중첩 JSON)를 서비스 계층과 분리하기 위해 평탄화한 형태다.
 */
@Getter
@Builder
@AllArgsConstructor
public class KakaoUserInfo {
    /** 카카오 회원 고유 식별자. User.providerId 로 저장한다. */
    private final Long id;

    /** 카카오 계정 이메일. 이메일 동의를 받지 못하면 null 일 수 있다. */
    private final String email;

    /** 카카오 프로필 닉네임. 동의를 받지 못하면 null 일 수 있다. */
    private final String nickname;
}
