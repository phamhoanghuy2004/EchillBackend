package com.echill.dto.response;

import com.echill.entity.enums.ConsultationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ConsultationResponse(
        @JsonFormat(shape =  JsonFormat.Shape.STRING)
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Integer birthYear,
        String topic,
        ConsultationStatus status,
        Instant createdAt,
        @JsonFormat(shape =  JsonFormat.Shape.STRING)
        Long handledById,
        String handledByName
) {}