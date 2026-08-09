package com.likelionknu.notdesign.user.data.dto.response;

import com.likelionknu.notdesign.user.data.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 응답 DTO.
 * 측정 동의 여부 등 회원의 현재 상태를 전달한다.
 */
@Getter
@Builder
public class UserResponseDto {
    private final Long id;
    private final String email;
    private final String name;
    private final LocalDateTime measurementAgreedAt;

    /**
     * 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param user 회원 엔티티
     * @return 변환된 응답 DTO
     */
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .measurementAgreedAt(user.getMeasurementAgreedAt())
                .build();
    }
}
