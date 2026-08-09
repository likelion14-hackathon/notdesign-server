package com.likelionknu.notdesign.result.data.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 측정 결과 생성 요청 DTO.
 * 방문한 클리닉 식별자를 선택적으로 받는다. 값이 있으면 생성되는 결과에 해당 클리닉을 연결하고,
 * 없으면 더미 측정값에 기록된 클리닉(없을 수 있음)을 그대로 사용한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultCreateRequestDto {
    /** 측정을 진행한 클리닉 ID(선택). */
    private Long clinicId;
}
