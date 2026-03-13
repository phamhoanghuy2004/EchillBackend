package com.echill.dto.response;

import com.echill.entity.enums.CertType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudyGoalResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    CertType certType;

    Double targetListening;
    Double targetReading;
    Double targetSpeaking;
    Double targetWriting;
    Double targetTotal;

    Double currentListening;
    Double currentReading;
    Double currentSpeaking;
    Double currentWriting;
    Double currentTotal;
}
