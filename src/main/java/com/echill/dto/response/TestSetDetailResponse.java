package com.echill.dto.response;

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
    Long id;
    String title;
    String description;
    Boolean isPublic;
    List<TestSummaryResponse> tests;
}
