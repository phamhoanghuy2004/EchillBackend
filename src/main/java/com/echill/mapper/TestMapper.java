package com.echill.mapper;

import com.echill.dto.response.TestResponse;
import com.echill.entity.Test;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {QuestionMapper.class})
public interface TestMapper {
    @Named("toResponse")
    @Mapping(source = "testSet.id", target = "testSetId")
    TestResponse toResponse(Test test);

    @IterableMapping(qualifiedByName = "toResponse")
    List<TestResponse> toResponseList(List<Test> tests);
}
