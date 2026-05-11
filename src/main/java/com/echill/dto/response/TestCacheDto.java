package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TestCacheDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long id,
        String title,
        Integer durationMinutes) {}