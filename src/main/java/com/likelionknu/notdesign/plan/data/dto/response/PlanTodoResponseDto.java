package com.likelionknu.notdesign.plan.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTodoResponseDto {
    private Long checklistId;
    private PlanCategory category;
    private String categoryName;
    private String content;
    private Boolean done;
}
