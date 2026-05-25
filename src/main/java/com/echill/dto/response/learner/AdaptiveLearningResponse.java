package com.echill.dto.response.learner;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdaptiveLearningResponse {

    /**
     * "LESSON_FOUND" | "UPSELL" | "NO_PROFILE" | "NO_GAP"
     */
    String status;

    // Thông tin Tag bị hổng
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long gapTagId;
    String gapTagName;
    Integer gapCurrentLevel;
    Integer gapTargetLevel;

    // Thông tin bài học (chỉ có khi status = LESSON_FOUND)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long lessonId;
    String lessonTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long courseId;
    String courseName;
    String courseImage;
}
