package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;

public record TestSetCacheDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long id,
        String title,
        String description,
        Boolean isPublic,
        List<TestCacheDto> tests
) {}