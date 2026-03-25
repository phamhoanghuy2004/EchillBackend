package com.echill.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCreationRequest {
    @NotNull(message = "COURSE_ID_REQUIRED")
    Long courseId;

    @NotBlank(message = "TITLE_REQUIRED")
    @Size(max = 200, message = "TITLE_TOO_LONG")
    String title;

    @NotBlank(message = "CONTENT_REQUIRED")
    String content;

    @NotNull(message = "DISPLAY_ORDER_REQUIRED")
    @Min(value = 0, message = "DISPLAY_ORDER_INVALID")
    Integer displayOrder;

    @NotNull(message = "PREVIEW_REQUIRED")
    Boolean isPreview;
}
