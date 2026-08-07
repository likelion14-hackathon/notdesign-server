package com.likelionknu.notdesign.diary.data.entity;

import com.likelionknu.notdesign.plan.data.entity.PlanTimeline;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "diary_todo")
public class DiaryTodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeline_id", nullable = false)
    private PlanTimeline timeline;

    @Builder.Default
    @Column(nullable = false)
    private Boolean done = false;
}
