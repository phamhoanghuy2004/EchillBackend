package com.echill.dto.response.learner;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter // Chỉ cần Getter để Jackson chuyển thành JSON, bỏ @Data cho nhẹ
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyCourseResponse {
    Long enrollmentId;
    Long courseId;
    String courseName;
    String courseImage;
    String teacherName;
    String teacherAvatar;
    Instant lastAccessedAt;

    // Thông số tiến độ
    Integer totalLessons;
    Long completedLessons;
    Integer progressPercent; // 💥 Tính toán động và gán vào đây
}
