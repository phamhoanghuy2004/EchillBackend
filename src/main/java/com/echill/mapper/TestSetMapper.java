package com.echill.mapper;

import com.echill.dto.request.TestSetRequest;
import com.echill.dto.response.TestSetDetailWithHistoryResponse;
import com.echill.dto.request.TestSetUpdateRequest;
import com.echill.dto.response.TestSetResponse;
import com.echill.entity.TestResult;
import com.echill.entity.TestSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {TestResultMapper.class})
public interface TestSetMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tests", ignore = true)
    TestSet toEntity(TestSetRequest request);

    @Mapping(source = "lesson.id", target = "lessonId")
    TestSetResponse toResponse(TestSet testSet);

    @Mapping(target = "testSetId", expression = "java(testSet.getId())")
    @Mapping(target = "maxAttempts", expression = "java(com.echill.constant.AppConstants.MAX_TEST_ATTEMPTS)")
    TestSetDetailWithHistoryResponse toDetailWithHistory(TestSet testSet, List<TestResult> history);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tests", ignore = true)
    void updateTestSet(@MappingTarget TestSet testSet, TestSetUpdateRequest request);
}
