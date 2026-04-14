package com.echill.service.evaluation;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AnswerEvaluator {
    public boolean isCorrect(Long userSelectedAnswerId, Set<Long> correctAnswersForQuestion) {
        if (userSelectedAnswerId == null || correctAnswersForQuestion == null || correctAnswersForQuestion.isEmpty()) {
            return false;
        }
        return correctAnswersForQuestion.contains(userSelectedAnswerId);
    }
}
