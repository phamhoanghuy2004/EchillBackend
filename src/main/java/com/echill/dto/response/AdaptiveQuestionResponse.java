package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdaptiveQuestionResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String content;
    private String audioUrl;
    private String imageUrl;

    // Giao tiếp với FE: Chỉ trả về list các đáp án (không có cờ đúng/sai)
    private List<AdaptiveAnswerResponse> answers;
}
