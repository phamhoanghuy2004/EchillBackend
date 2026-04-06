package com.echill.mapper;

import com.echill.dto.response.QuestionResponse;
import com.echill.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AnswerMapper.class})
public interface QuestionMapper {
    @Mapping(source = "tag.name", target = "tagName")
    QuestionResponse toResponse(Question question);
}
