package com.echill.dto.response.learner;

import com.echill.entity.enums.LessonStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter // Chỉ cần Getter để Jackson chuyển thành JSON, bỏ @Data cho nhẹ
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonItemResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long lessonId;
    String title;
    Integer displayOrder;
    Long durationSeconds;

    // Phân loại tài nguyên của bài học
    Boolean hasVideo;
    Boolean hasDocument;
    Boolean hasTest;

    // 💥 THÔNG TIN TIẾN ĐỘ (Real-time từ SQL)
    LessonStatus status; // NOT_STARTED, IN_PROGRESS, COMPLETED, OUTDATED (Bắt học lại)
    Integer lastWatchedSecond;
}
