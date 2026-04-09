package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResultHistoryResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long testId;
    String testTitle;

    Double totalScore;
    Double listeningScore;
    Double readingScore;
    Double speakingScore;
    Double writingScore;

    Integer timeTakenSeconds;
    Boolean isPassed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;
}
