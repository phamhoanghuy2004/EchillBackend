package com.echill.event;

public record QuizPassedEvent(
        Long studentId,
        Long testSetId
) {}