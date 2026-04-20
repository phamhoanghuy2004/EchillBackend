package com.echill.dto.response;

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
public class QuestionResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String content;
    String explanation;
    SkillType skillType;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long tagId;
    String tagName;
    Integer orderIndex;

    QuestionGroupResponse group;

    List<AnswerResponse> answers;
}
