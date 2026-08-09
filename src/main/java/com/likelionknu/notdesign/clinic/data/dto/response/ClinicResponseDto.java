package com.likelionknu.notdesign.clinic.data.dto.response;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import lombok.Builder;
import lombok.Getter;

/**
 * 클리닉 정보 응답 DTO.
 */
@Getter
@Builder
public class ClinicResponseDto {
    private final Long id;
    private final String name;
    private final String address;

    /**
     * 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param clinic 클리닉 엔티티
     * @return 변환된 응답 DTO
     */
    public static ClinicResponseDto from(Clinic clinic) {
        return ClinicResponseDto.builder()
                .id(clinic.getId())
                .name(clinic.getName())
                .address(clinic.getAddress())
                .build();
    }
}
