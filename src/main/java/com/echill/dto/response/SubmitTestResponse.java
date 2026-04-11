package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitTestResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long resultId;
    Double score;
    Integer correctAnswers;
    Integer totalQuestions;
    Boolean isLate;
    Boolean isPassed;
    String message;
}
