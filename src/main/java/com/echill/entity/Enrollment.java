package com.echill.entity;

import com.echill.entity.enums.EnrollmentStatus;
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
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "course_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Enrollment extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Course course;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    Double progressPercent = 0.0;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    Boolean isCompleted = false;

    @Column(name = "enrollment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    EnrollmentStatus enrollmentStatus = EnrollmentStatus.ACTIVE;

    @Column(name = "last_accessed_at", nullable = false)
    Instant lastAccessedAt;

    // ==========================================
    // HELPER METHODS (Đóng gói nghiệp vụ Học tập)
    // ==========================================

    /**
     * Tự động set thời gian truy cập lần đầu khi mới mua khóa học
     */
    @PrePersist
    protected void onCreate() {
        if (this.lastAccessedAt == null) {
            this.lastAccessedAt = Instant.now();
        }
    }

    /**
     * Ghi nhận lại thời điểm học viên vừa bấm vào học
     */
    public void recordAccess() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Cập nhật phần trăm tiến độ và tự động cấp chứng chỉ/hoàn thành
     */
    public void updateProgress(Double percent) {
        if (percent == null || percent < 0.0 || percent > 100.0) {
            throw new IllegalArgumentException("Phần trăm tiến độ không hợp lệ (phải từ 0 đến 100)");
        }

        this.progressPercent = percent;

        // Tự động chốt sổ nếu đạt 100%
        if (this.progressPercent >= 100.0) {
            this.isCompleted = true;
        }
    }

    public void unlockCourse() {
        if (this.enrollmentStatus == EnrollmentStatus.LOCKED) {
            this.enrollmentStatus = EnrollmentStatus.ACTIVE;
        }
    }

    /**
     * Dành cho luồng: Mua Combo, khóa đầu tiên ACTIVE, các khóa sau bị khóa lại chờ học
     */
    public void lockCourse() {
        this.enrollmentStatus = EnrollmentStatus.LOCKED;
    }
}
