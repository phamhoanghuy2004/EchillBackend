package com.echill.mapper;

import com.echill.dto.response.TestResultHistoryResponse;
import com.echill.entity.TestResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TestResultMapper {

    @Mapping(source = "test.id", target = "testId")
    @Mapping(source = "test.title", target = "testTitle")
    TestResultHistoryResponse toHistoryResponse(TestResult result);

    default LocalDateTime map(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
