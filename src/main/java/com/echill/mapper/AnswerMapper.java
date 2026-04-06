package com.echill.mapper;

import com.echill.dto.response.AnswerResponse;
import com.echill.entity.Answer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnswerMapper {
    AnswerResponse toResponse(Answer answer);
}
