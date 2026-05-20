package com.echill.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitAnswerRequest {

    @NotNull(message = "ID câu hỏi không được để trống")
    Long questionId;

    // Có thể null nếu user hết giờ/bỏ qua câu hỏi
    Long selectedAnswerId;
}