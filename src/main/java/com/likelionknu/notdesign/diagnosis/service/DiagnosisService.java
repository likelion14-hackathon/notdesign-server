package com.likelionknu.notdesign.diagnosis.service;

import com.likelionknu.notdesign.diagnosis.data.dto.request.DiagnosisCreateRequestDto;
import com.likelionknu.notdesign.diagnosis.data.dto.response.DiagnosisResponseDto;
import com.likelionknu.notdesign.diagnosis.data.entity.Diagnosis;
import com.likelionknu.notdesign.diagnosis.data.enums.Attribution;
import com.likelionknu.notdesign.diagnosis.data.enums.FeltEffect;
import com.likelionknu.notdesign.diagnosis.data.enums.UnderstandingGrade;
import com.likelionknu.notdesign.diagnosis.data.repository.DiagnosisRepository;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiagnosisService {
    private final UserRepository userRepository;
    private final DiagnosisRepository diagnosisRepository;

    private static final int MONTHS_PER_YEAR = 12;

    /**
     * 웰니스 지출 진단을 생성합니다.
     * 설문 응답으로 낭비율·낭비액과 기여 이해도 등급을 계산해 저장하고 결과를 반환합니다.
     *
     * @param email 진단을 요청한 사용자
     * @param request 시술·제품 지출과 개선 체감도·기여도 파악 응답
     * @return 총 지출, 월 평균, 등급, 낭비액
     */
    @Transactional
    public DiagnosisResponseDto createDiagnosis(String email, DiagnosisCreateRequestDto request) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        FeltEffect feltEffect = request.getFeltEffect();
        Attribution attribution = request.getAttribution();

        int totalCost = request.getProcedureCost() + request.getProductCost();
        int monthlyAverage = Math.round((float) totalCost / MONTHS_PER_YEAR);
        // 낭비율 = 기본 누수(체감도) + 활용 누수(기여도 파악)
        int wasteRate = feltEffect.getBaseLeak() + attribution.getUtilLeak();
        int wasteAmount = totalCost * wasteRate / 100;
        UnderstandingGrade grade = UnderstandingGrade.from(feltEffect.getScore() + attribution.getScore());

        Diagnosis diagnosis = diagnosisRepository.save(
                Diagnosis.builder()
                        .user(user)
                        .procedureCost(request.getProcedureCost())
                        .productCost(request.getProductCost())
                        .feltEffect(feltEffect)
                        .attribution(attribution)
                        .grade(grade)
                        .wasteRate(wasteRate)
                        .wasteAmount(wasteAmount)
                        .build());

        return DiagnosisResponseDto.builder()
                .diagnosisId(diagnosis.getId())
                .totalCost(totalCost)
                .monthlyAverage(monthlyAverage)
                .grade(grade)
                .gradeName(grade.getDisplayName())
                .wasteAmount(wasteAmount)
                .build();
    }
}
