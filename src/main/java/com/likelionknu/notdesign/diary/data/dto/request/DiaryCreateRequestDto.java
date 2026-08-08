package com.likelionknu.notdesign.diary.data.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCreateRequestDto {
    @NotNull
    @Min(0)
    @Max(10)
    private Integer skinTone;

    @NotNull
    @Min(0)
    @Max(10)
    private Integer tightnessAndDryness;

    @NotNull
    @Min(0)
    @Max(10)
    private Integer flushing;

    @Size(max = 100)
    private String comment;

    private List<Long> doneChecklistIds;
}
