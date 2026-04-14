package com.echill.dto.response.guest;

import com.echill.dto.response.TestResultHistoryResponse;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestReviewDetailResponse {
    TestResultHistoryResponse summary;
    TestPracticeResponse testData;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    Map<Long, Long> userAnswers;
}

