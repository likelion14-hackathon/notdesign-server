package com.likelionknu.notdesign.clinic.service;

import com.likelionknu.notdesign.clinic.data.dto.response.ClinicResponseDto;
import com.likelionknu.notdesign.clinic.data.repository.ClinicRepository;
import com.likelionknu.notdesign.common.response.GlobalResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클리닉 도메인 서비스.
 * 제휴 클리닉 목록 조회를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ClinicService {
    private final ClinicRepository clinicRepository;

    /**
     * 전체 클리닉 목록을 조회한다.
     *
     * @return 클리닉 목록
     */
    @Transactional(readOnly = true)
    public GlobalResponse<List<ClinicResponseDto>> getClinics() {
        List<ClinicResponseDto> clinics = clinicRepository.findAll().stream()
                .map(ClinicResponseDto::from)
                .toList();

        return GlobalResponse.ok(clinics);
    }
}
