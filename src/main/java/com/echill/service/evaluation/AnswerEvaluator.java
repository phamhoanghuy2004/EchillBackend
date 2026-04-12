package com.echill.service.evaluation;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AnswerEvaluator {
    public boolean isCorrect(Long userSelectedAnswerId, Set<Long> correctAnswersForQuestion, boolean isLate) {
        if (isLate || userSelectedAnswerId == null || correctAnswersForQuestion == null) {
            return false;
        }
        // Sau này nếu có multi-choice (chọn nhiều đáp án 1 câu), logic thay đổi ở đây mà không rách Service
        return correctAnswersForQuestion.contains(userSelectedAnswerId);
    }
}
