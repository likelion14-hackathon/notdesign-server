package com.likelionknu.notdesign.plan.data.dto.response;

import com.likelionknu.notdesign.plan.data.enums.PlanCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDetailItemResponseDto {
    private PlanCategory category;
    private String categoryName;
    private String name;
    private String frequency;
    private Integer price;
    private List<Integer> weeks;
    private String reason;
}
