package com.echill.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;


@Entity
// BẢO VỆ DATABASE: Đảm bảo 1 lần ghi danh chỉ có 1 tiến độ cho 1 bài học
@Table(name = "lesson_progresses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"enrollment_id", "lesson_id"})},
        indexes = {
                @Index(name = "idx_progress_calc_full", columnList = "enrollment_id, is_completed, version_completed")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgress extends BaseEntity {
    @Id
    @Tsid
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

    @Column(name = "version_completed")
    private Integer versionCompleted;

    @Column(name = "completed_at")
    Instant completedAt;

    // ==========================================
    // HELPER METHODS (Đóng gói nghiệp vụ Tiến độ)
    // ==========================================

    /**
     * Lưu lại số giây đang xem dở (Resume play)
     * Chỉ lưu khi số giây mới lớn hơn số giây cũ để tránh lỗi tua ngược video
     */
    public void updateWatchTime(Integer currentSecond) {
        if (currentSecond != null && currentSecond > this.lastWatchedSecond) {
            this.lastWatchedSecond = currentSecond;
        }
    }

    /**
     * Đánh dấu bài học này đã hoàn thành toàn bộ (Xem xong Video + Pass Quiz)
     */
    public void markAsCompleted(Integer currentLessonVersion) {
        this.isVideoWatched = true;
        this.isQuizPassed = true;
        this.isCompleted = true;
        this.versionCompleted = currentLessonVersion; // CHỐT SỔ VERSION LÚC HOÀN THÀNH

        if (this.completedAt == null) {
            this.completedAt = Instant.now();
        }
    }

    /**
     * Đánh dấu đã xem xong Video (Dùng cho bài không có bài tập)
     */
    public void markVideoWatched() {
        this.isVideoWatched = true;
    }

    /**
     * Đánh dấu đã qua bài kiểm tra nhỏ (Dùng cho bài Quiz)
     */
    public void markQuizPassed() {
        this.isQuizPassed = true;
    }

    /**
     * Helper kiểm tra xem tiến độ này đã bị Outdated (Lỗi thời) do Teacher update bài chưa?
     * True = Hợp lệ (Tính vào %). False = Bắt học lại (Không tính %).
     */
    public boolean isValidCompletion(Integer currentLessonVersion) {
        return Boolean.TRUE.equals(this.isCompleted)
                && this.versionCompleted != null
                && this.versionCompleted.equals(currentLessonVersion);
    }
}
