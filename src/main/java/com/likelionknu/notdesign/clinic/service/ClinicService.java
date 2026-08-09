package com.likelionknu.notdesign.clinic.service;

import com.likelionknu.notdesign.clinic.data.dto.response.ClinicResponseDto;
import com.likelionknu.notdesign.clinic.data.repository.ClinicRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicService {
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public List<ClinicResponseDto> getClinics() {
        return clinicRepository.findAll().stream()
                .map(ClinicResponseDto::from)
                .toList();
    }
}
