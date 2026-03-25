package com.echill.dto.response;

import com.echill.entity.enums.VideoStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String content;
    Integer displayOrder;
    Boolean isPreview;
    String publicVideoId;
    String rawUrl;
    String hlsUrl;
    VideoStatus videoStatus;
    Long durationSeconds;

    // Chỉ lấy ID của Course cha
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long courseId;

    // 💥 CHỨA DANH SÁCH THẰNG CON (Đã được làm phẳng)
    List<DocumentResponse> documents;
}
