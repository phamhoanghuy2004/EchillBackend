package com.echill.dto.response.learner;

import com.echill.entity.enums.VideoStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter // Chỉ cần Getter để Jackson chuyển thành JSON, bỏ @Data cho nhẹ
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonDetailResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String content;
    String hlsUrl;
    VideoStatus videoStatus;

    List<DocumentDto> documents;

    TestSetShortcutDto testSet;
}
