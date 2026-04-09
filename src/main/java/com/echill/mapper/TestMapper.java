package com.echill.mapper;

import com.echill.dto.request.TestUpdateRequest;
import com.echill.dto.response.TestResponse;
import com.echill.entity.Test;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {QuestionMapper.class})
public interface TestMapper {
    @Named("toResponse")
    @Mapping(source = "testSet.id", target = "testSetId")
    TestResponse toResponse(Test test);

    @IterableMapping(qualifiedByName = "toResponse")
    List<TestResponse> toResponseList(List<Test> tests);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "testSet", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateTest(@MappingTarget Test test, TestUpdateRequest request);
}
