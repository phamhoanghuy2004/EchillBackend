package com.echill.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonResponse {
    Long id;
    String title;
    String content;
    Integer displayOrder;
    Boolean isPreview;
    String videoUrl;
    Long durationSeconds;
}
