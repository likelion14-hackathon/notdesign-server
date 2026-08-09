package com.likelionknu.notdesign.clinic.data.dto.response;

import com.likelionknu.notdesign.clinic.data.entity.Clinic;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClinicResponseDto {
    private final Long id;
    private final String name;
    private final String address;

    public static ClinicResponseDto from(Clinic clinic) {
        return ClinicResponseDto.builder()
                .id(clinic.getId())
                .name(clinic.getName())
                .address(clinic.getAddress())
                .build();
    }
}
