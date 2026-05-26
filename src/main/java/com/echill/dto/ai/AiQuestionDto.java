package com.echill.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiQuestionDto(
        String content,
        @JsonProperty("optionA") String optionA,
        @JsonProperty("optionB") String optionB,
        @JsonProperty("optionC") String optionC,
        @JsonProperty("optionD") String optionD,
        String correctAnswer,
        String explanation
) {
}
