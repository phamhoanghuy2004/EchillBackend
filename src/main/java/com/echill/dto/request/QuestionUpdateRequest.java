package com.echill.dto.request;

import com.echill.entity.enums.SkillType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionUpdateRequest {
    @NotBlank(message = "QUESTION_CONTENT_REQUIRED")
    String content;

    String explanation;

    SkillType skillType;

    String tagName;

    @NotEmpty(message = "ANSWERS_CANNOT_BE_EMPTY")
    @Valid
    List<AnswerRequest> answers;
}