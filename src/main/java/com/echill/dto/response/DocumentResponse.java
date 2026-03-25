package com.echill.dto.response;

import com.echill.entity.enums.FileType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String fileUrl;
    FileType fileType;

    // 💥 CHỈ LẤY ID CỦA CHA, KHÔNG LẤY OBJECT
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long lessonId;
}
