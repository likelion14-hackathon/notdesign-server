package com.likelionknu.notdesign.user.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProvider {
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String displayName;
}
