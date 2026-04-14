package com.echill.service.impl;

import com.echill.dto.response.guest.AnswerPracticeResponse;
import com.echill.dto.response.guest.QuestionPracticeResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.dto.response.guest.TestSectionPracticeResponse;
import com.echill.service.evaluation.AnswerEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestEvaluationService {

    private final AnswerEvaluator answerEvaluator;

    public EvaluationContext evaluateWithSnapshot(TestPracticeResponse snapshotTest, Map<Long, Long> userAnswersRaw) {
        int totalQuestions = 0;
        int correctCount = 0;
        Set<Long> correctAnsIds = new HashSet<>();

        if (snapshotTest.getSections() != null) {
            for (TestSectionPracticeResponse section : snapshotTest.getSections()) {
                if (section.getQuestions() != null) {
                    for (QuestionPracticeResponse question : section.getQuestions()) {
                        totalQuestions++;

                        correctAnsIds.clear();

                        if (question.getAnswers() != null) {
                            for (AnswerPracticeResponse ans : question.getAnswers()) {
                                if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                                    correctAnsIds.add(ans.getId());
                                }
                            }
                        }

                        Long userSelected = userAnswersRaw.get(question.getId());

                        if (answerEvaluator.isCorrect(userSelected, correctAnsIds)) {
                            correctCount++;
                        }
                    }
                }
            }
        }

        return new EvaluationContext(correctCount, totalQuestions);
    }

    public record EvaluationContext(int correctCount, int totalQuestions) {}
}