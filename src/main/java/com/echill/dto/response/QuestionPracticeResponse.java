package com.echill.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionPracticeResponse {
    Long id;
    String content;
    String audioUrl;
    String imageUrl;

    // 🟢 Đã sửa thành 1 ID duy nhất (Vì 1 câu hỏi chỉ map với 1 Tag)
    Long childTagId;

    // Lưu đáp án CÓ KÈM CỜ ĐÚNG SAI để Backend tự chấm
    List<AnswerPracticeResponse> answers;
}
