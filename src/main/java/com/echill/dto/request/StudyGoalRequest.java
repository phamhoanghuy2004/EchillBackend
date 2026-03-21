package com.echill.dto.request;

import com.echill.entity.enums.CertType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Range;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudyGoalRequest {

    @NotNull(message = "CERT_TYPE_REQUIRED")
    CertType certType;

    @NotNull(message = "SCORE_REQUIRED")
    @Range(min = 0, max = 495, message = "LISTENING_SCORE_INVALID")
    Double targetListening;

    @NotNull(message = "SCORE_REQUIRED")
    @Range(min = 0, max = 495, message = "READING_SCORE_INVALID")
    Double targetReading;

    @NotNull(message = "SCORE_REQUIRED")
    @Range(min = 0, max = 200, message = "SPEAKING_SCORE_INVALID")
    Double targetSpeaking;

    @NotNull(message = "SCORE_REQUIRED")
    @Range(min = 0, max = 200, message = "WRITING_SCORE_INVALID")
    Double targetWriting;

    @NotNull(message = "SCORE_REQUIRED")
    @Range(min = 0, max = 990, message = "TOTAL_SCORE_INVALID") // Tổng điểm tối đa của TOEIC LR là 990
    Double targetTotal;
}
