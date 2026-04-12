package com.echill.dto.response.guest;

import com.echill.entity.enums.SkillType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionPracticeResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String content;
    SkillType skillType;
    String tagName;
    Integer orderIndex;

    QuestionGroupPracticeResponse group;
    List<AnswerPracticeResponse> answers;
}
