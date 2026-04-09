package com.echill.mapper;

import com.echill.dto.response.TestSectionResponse;
import com.echill.entity.TestSection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {QuestionMapper.class})
public interface TestSectionMapper {
    TestSectionResponse toResponse(TestSection section);
}
