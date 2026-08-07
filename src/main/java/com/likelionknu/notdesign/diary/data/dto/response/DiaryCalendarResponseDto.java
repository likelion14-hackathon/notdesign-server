package com.likelionknu.notdesign.diary.data.dto.response;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCalendarResponseDto {
    private LocalDate date;
    private Boolean recorded;
}
