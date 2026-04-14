package com.echill.dto.response;

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
public class TestSetDetailWithHistoryResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long testSetId;
    String title;
    String description;
    Boolean isPublic;
    Integer year;
    Integer maxAttempts;

    List<TestResultHistoryResponse> history;
}
