package com.echill.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCreationRequest {
    @NotNull(message = "COURSE_ID_REQUIRED")
    Long courseId;

    @NotBlank(message = "LESSON_TITLE_REQUIRED")
    @Size(max = 200, message = "LESSON_TITLE_TOO_LONG")
    String title;

    @NotBlank(message = "LESSON_CONTENT_REQUIRED")
    String content;

    @NotNull(message = "LESSON_DISPLAY_ORDER_REQUIRED")
    @Min(value = 0, message = "LESSON_DISPLAY_ORDER_INVALID")
    Integer displayOrder;

    @NotNull(message = "LESSON_PREVIEW_REQUIRED")
    Boolean isPreview;

    List<Long> tagIds;
}
