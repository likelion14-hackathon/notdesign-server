package com.likelionknu.notdesign.user.data.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오 소셜 로그인 요청 DTO.
 * 프론트엔드가 카카오 인가(authorization) 과정을 거쳐 발급받은 인가 코드를 전달한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequestDto {
    /** 카카오 인가 코드. 이 코드로 백엔드가 카카오 액세스 토큰을 교환한다. */
    @NotBlank
    private String code;
}
