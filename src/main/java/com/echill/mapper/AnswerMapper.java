package com.echill.mapper;

import com.echill.dto.request.AnswerRequest;
import com.echill.dto.response.AnswerResponse;
import com.echill.entity.Answer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AnswerMapper {
    AnswerResponse toResponse(Answer answer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateAnswer(@MappingTarget Answer answer, AnswerRequest request);
}
