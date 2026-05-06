package com.echill.mapper;

import com.echill.dto.response.QuestionGroupResponse;
import com.echill.entity.QuestionGroup;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {QuestionMapper.class})
public interface QuestionGroupMapper {
    QuestionGroupResponse toResponse(QuestionGroup group);
}
