package com.echill.dto.response.learner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter // Chỉ cần Getter để Jackson chuyển thành JSON, bỏ @Data cho nhẹ
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurriculumResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long courseId;
    String courseName;
    Integer totalLessons;
    Long completedLessons;
    Integer progressPercent;

    // Danh sách bài học để FE vẽ Menu
    List<LessonItemResponse> lessons;
}
