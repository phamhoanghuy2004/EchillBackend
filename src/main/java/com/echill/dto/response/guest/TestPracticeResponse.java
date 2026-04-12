package com.echill.dto.response.guest;

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
public class TestPracticeResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long sessionId;

    String title;
    TestType type;
    Integer durationMinutes;
    Double passScore;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long testSetId;

    List<TestSectionPracticeResponse> sections;
}
