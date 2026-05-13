package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TestSectionSummaryDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long id,
        String title,
        Integer orderIndex,
        Long totalQuestions
) {}