package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record TestResultHistoryDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long testId,
        String testTitle,
        Double totalScore,
        Integer timeTakenSeconds,
        Boolean isPassed,
        Instant createdAt
) {}