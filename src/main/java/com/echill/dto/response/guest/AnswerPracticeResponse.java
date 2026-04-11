package com.echill.dto.response.guest;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerPracticeResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String content;
}
