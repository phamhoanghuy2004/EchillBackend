package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerRequest {
    Long id; // null => tạo mới, có giá trị => cập nhật theo ID

    @NotBlank(message = "ANSWER_CONTENT_REQUIRED")
    String content;

    @NotNull(message = "IS_CORRECT_REQUIRED")
    Boolean isCorrect;
}
