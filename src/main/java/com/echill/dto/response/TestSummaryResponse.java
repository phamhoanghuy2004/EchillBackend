package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSummaryResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
    Integer durationMinutes;
    Integer price;           // 0 nếu public, 10 nếu private
    Long totalQuestions;     // Tổng số câu hỏi
    Boolean hasAttempted;    // True nếu user đã từng làm
}
