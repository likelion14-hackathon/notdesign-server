package com.likelionknu.notdesign.diary.data.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponseDto {
    private Long diaryId;
    private Integer skinTone;
    private Integer pores;
    private Integer flushing;
    private String comment;
    private List<DiaryTodoResponseDto> todos;
}
