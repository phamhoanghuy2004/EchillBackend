package com.echill.dto.response;

import com.echill.entity.enums.TestType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSetResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String description;
    Boolean isPublic;
    Integer year;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long lessonId;

    TestType type;
    Integer price;
}
