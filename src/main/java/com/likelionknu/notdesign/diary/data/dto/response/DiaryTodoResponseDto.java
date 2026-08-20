package com.likelionknu.notdesign.diary.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTodoResponseDto {
    private Long checklistId;
    private PlanCategory category;
    private String categoryName;
    private String content;
    private Boolean done;
}
