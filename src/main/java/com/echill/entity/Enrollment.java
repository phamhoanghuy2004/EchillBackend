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

    @Column(name = "completed_lessons_count", nullable = false)
    @Builder.Default
    Integer completedLessonsCount = 0;

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

    public void recordAccess() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * 💥 2. HÀM MỚI: Tăng số lượng bài học hoàn thành lên 1
     */
    public void incrementCompletedLesson() {
        this.completedLessonsCount++;
        checkCourseCompletion();
    }

    /**
     * HÀM MỚI: Dành cho trường hợp Giảng viên update video bắt học lại -> Bị trừ đi 1 bài
     */
    public void decrementCompletedLesson() {
        if (this.completedLessonsCount > 0) {
            this.completedLessonsCount--;
        }
        checkCourseCompletion(); // Có thể bị tụt từ Đã Hoàn Thành -> Chưa Hoàn thành
    }

    /**
     * Kiểm tra chốt sổ chứng chỉ, nếu mà số bài học đã hoàn thành enrollment bằng với tổng số bài học ở khóa học
     * thì đánh dấu là nó đã hoàn thành
     */
    private void checkCourseCompletion() {
        if (this.course != null && this.course.getTotalLessonsCount() != null && this.course.getTotalLessonsCount() > 0) {
            this.isCompleted = this.completedLessonsCount >= this.course.getTotalLessonsCount();
        }
    }

    /**
     * 💥 3. TUYỆT CHIÊU: Trả về phần trăm tiến độ bằng cách tính toán ảo trên RAM
     * Hàm này sẽ được gọi khi bạn Map sang DTO trả về cho Frontend
     */
    @Transient
    public Double getDerivedProgressPercent() {
        if (this.course == null || this.course.getTotalLessonsCount() == null || this.course.getTotalLessonsCount() == 0) {
            return 0.0;
        }

        // Tránh lỗi chia vượt quá 100% nếu logic có chút sai sót (Safe fallback)
        if (this.completedLessonsCount >= this.course.getTotalLessonsCount()) {
            return 100.0;
        }

        // Công thức: (Số bài đã xong * 100) / Tổng số bài
        return (this.completedLessonsCount * 100.0) / this.course.getTotalLessonsCount();
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
