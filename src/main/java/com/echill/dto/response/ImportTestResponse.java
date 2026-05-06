package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportTestResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long testId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long testSetId;
    String testTitle;
    int totalQuestions;
    int totalGroups;
    String status; // "SUCCESS"
}
