package com.echill.dto.response.learner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor // 🔴 Rất quan trọng để JPQL tạo object
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSetRecommendationResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    String description;
    Integer year;
    Integer totalTests; // Tổng số bài test trong bộ đề này
}