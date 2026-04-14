package com.echill.dto.response.guest;

import com.echill.entity.enums.SkillType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public class QuestionPracticeResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String content;
    SkillType skillType;
    String tagName;
    Integer orderIndex;

    QuestionGroupPracticeResponse group;
    List<AnswerPracticeResponse> answers;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String explanation;
}
