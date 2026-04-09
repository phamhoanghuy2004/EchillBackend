package com.echill.dto.response.guest;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonPublicResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String content;
    Integer displayOrder;
    Boolean isPreview;
    Long durationSeconds;
    String previewVideoUrl;
    List<DocumentPublicResponse> documents;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long testSetId;
    Boolean hasTest;
}
