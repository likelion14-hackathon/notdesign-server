package com.likelionknu.notdesign.diagnosis.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UnderstandingGrade {
    HIGH("높음",
            "본인에게 맞는 관리를 정확히 알고 있지만, 최적의 주기나 시술 간의 시너지 횟수까지 수치화하여 계산하고 있지는 않을 확률이 높습니다.",
            "이미 훌륭한 루틴이지만 피부 상태에 맞춘 유동적인 조절없이 고정적으로 소비하는 경우, 총 지출의 %d%%인 약 %,d원이 오버스펙으로 지출되고 있을 수 있습니다."),
    MEDIUM("보통",
            "나름의 기준을 갖고 관리하고 있지만, 여러 관리가 동시에 진행될 때 각각의 정확한 피부 개선 기여도를 수치로 분리해서 파악하기는 어려운 상태입니다.",
            "효과가 둔화된 관리를 습관적으로 유지하거나 과중복된 루틴을 방치할 경우, 총 지출의 %d%%인 %,d원의 예산이 비효율적으로 활용될 수 있습니다."),
    LOW("낮음",
            "피부 관리를 꾸준히 진행 중이지만, 어떤 관리 덕분에 좋아졌는지 판단하는 기준이 감과 추측에 머물러 있어 기여 이해도는 낮음 수준입니다.",
            "주관적인 추측으로 관리를 계속할 경우, 나에게 실제 효과가 적은 관리에 총 지출의 %d%%인 약 %,d원이 불필요하게 새어나가고 있을 확률이 높습니다."),
    VERY_LOW("매우 낮음",
            "피부 개선은 어느 정도 체감하고 있지만, 어떤 관리 덕분인지 피부 개선 기여도를 파악하고 이해하는 정도는 매우 낮음입니다.",
            "피부 개선 기여 이해도가 매우 낮은 채 소비하는 현재, 총 지출의 %d%%인 약 %,d원이 효과 없는 곳에 낭비되고 있을 확률이 높습니다.");

    private final String displayName;
    private final String problemDiagnosis;
    private final String wasteDescription;

    public static UnderstandingGrade from(int score) {
        if (score >= 12) {
            return HIGH;
        }
        if (score >= 11) {
            return MEDIUM;
        }
        if (score >= 5) {
            return LOW;
        }
        return VERY_LOW;
    }

    public String formatWasteDescription(int wasteRate, int wasteAmount) {
        return String.format(wasteDescription, wasteRate, wasteAmount);
    }
}
