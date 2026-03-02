package com.echill.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Entity
// BẢO VỆ DATABASE: Đảm bảo 1 lần ghi danh chỉ có 1 tiến độ cho 1 bài học
@Table(name = "lesson_progresses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"enrollment_id", "lesson_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgress extends BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Lesson lesson;

    @Column(name = "is_video_watched", nullable = false)
    @Builder.Default
    Boolean isVideoWatched = false;

    @Column(name = "last_watched_second", nullable = false)
    @Builder.Default
    Integer lastWatchedSecond = 0; // Lưu lại số giây đang xem dở (Resume play)

    @Column(name = "is_quiz_passed", nullable = false)
    @Builder.Default
    Boolean isQuizPassed = false;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    Boolean isCompleted = false;
}
