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
// BẢO VỆ DATABASE: Chống Spam. Một user chỉ được đánh giá 1 khóa học đúng 1 lần.
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review extends BaseEntity {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(nullable = false)
    Double rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Học viên bị xóa -> Các đánh giá của họ tự bốc hơi
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Khóa học bị xóa -> Quét sạch toàn bộ đánh giá của khóa đó
    Course course;

    // ==========================================
    // HELPER METHODS (Đóng gói nghiệp vụ Đánh giá)
    // ==========================================

    /**
     * Hàm dùng khi học viên tạo mới hoặc chỉnh sửa đánh giá
     */
    public void updateReview(Double rating, String content) {
        if (rating == null || rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Điểm đánh giá (Rating) phải nằm trong khoảng từ 1.0 đến 5.0");
        }
        this.rating = rating;
        this.content = content;
    }

    /**
     * Vũ khí bí mật của JPA: Tự động chạy hàm này ngay trước khi INSERT hoặc UPDATE xuống Database.
     * Đảm bảo 100% không có dữ liệu rác nào lọt qua được dù tầng Service có code ẩu đến đâu.
     */
    @PrePersist
    @PreUpdate
    protected void validateRating() {
        if (this.rating == null || this.rating < 1.0 || this.rating > 5.0) {
            throw new IllegalStateException("Hệ thống từ chối lưu: Điểm đánh giá không hợp lệ!");
        }
    }
}
