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
public class TestSetDetailResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String description;
    Boolean isPublic;
    List<TestSummaryResponse> tests;
}
