package com.echill.mapper;

import com.echill.dto.request.QuestionUpdateRequest;
import com.echill.dto.response.QuestionResponse;
import com.echill.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AnswerMapper.class})
public interface QuestionMapper {
    @Mapping(source = "tag.name", target = "tagName")
    @Mapping(source = "questionGroup", target = "group")
    QuestionResponse toResponse(Question question);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "section", ignore = true)
    @Mapping(target = "questionGroup", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "tag", ignore = true)
    @Mapping(target = "answers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "audioUrl", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    void updateQuestion(@MappingTarget Question question, QuestionUpdateRequest request);
}