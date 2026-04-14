package com.echill.entity;

import com.echill.entity.enums.EnrollmentStatus;
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
        @UniqueConstraint(columnNames = {"student_id", "course_id"})},
        indexes = {
                @Index(name = "idx_student_access", columnList = "student_id, last_accessed_at DESC")
        }
)
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

    public void recordAccess() {
        this.lastAccessedAt = Instant.now();
    }


    public void unlockCourse() {
        if (this.enrollmentStatus == EnrollmentStatus.LOCKED) {
            this.enrollmentStatus = EnrollmentStatus.ACTIVE;
        }
    }

    public void lockCourse() {
        this.enrollmentStatus = EnrollmentStatus.LOCKED;
    }
}
