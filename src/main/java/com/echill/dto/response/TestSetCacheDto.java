package com.echill.dto.response;

import java.util.List;

public record TestSetCacheDto(
        Long id,
        String title,
        String description,
        Boolean isPublic,
        List<TestCacheDto> tests
) {}