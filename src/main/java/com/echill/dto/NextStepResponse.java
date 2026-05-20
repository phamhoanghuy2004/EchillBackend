package com.echill.dto;

import com.echill.dto.response.AdaptiveQuestionResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NextStepResponse {
    boolean isFinished; // True nếu đã test xong toàn bộ, False nếu còn câu hỏi
    AdaptiveQuestionResponse nextQuestion;; // Null nếu isFinished = true
    String message;
}
