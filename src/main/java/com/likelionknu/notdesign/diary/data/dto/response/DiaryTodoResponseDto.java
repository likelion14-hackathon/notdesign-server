package com.likelionknu.notdesign.diary.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTodoResponseDto {
    private PlanCategory category;
    private String categoryName;
    private String name;
    private Boolean done;
}
