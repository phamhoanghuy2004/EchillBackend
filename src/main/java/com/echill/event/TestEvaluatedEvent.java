package com.echill.event;

import com.echill.entity.enums.TestType;

import java.util.Map;

public record TestEvaluatedEvent(
        Long userId,
        Map<Long, Integer> tagLevelScores,
        TestType testType
) {}