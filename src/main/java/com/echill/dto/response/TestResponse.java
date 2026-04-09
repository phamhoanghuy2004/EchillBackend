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
public class TestResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    TestType type;
    Integer durationMinutes;
    Double passScore;
    Long testSetId;
    List<TestSectionResponse> sections;
}
