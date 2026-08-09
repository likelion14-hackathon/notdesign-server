package com.likelionknu.notdesign.user.data.dto.response;

import com.likelionknu.notdesign.user.data.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
    private final Long id;
    private final String email;
    private final String name;
    private final LocalDateTime measurementAgreedAt;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .measurementAgreedAt(user.getMeasurementAgreedAt())
                .build();
    }
}
