package com.echill.dto.request;

import com.echill.entity.enums.SkillType;
import jakarta.validation.constraints.NotBlank;
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

    List<AnswerRequest> answers;
}
