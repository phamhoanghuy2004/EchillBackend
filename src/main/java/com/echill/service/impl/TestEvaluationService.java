package com.echill.service.impl;

import com.echill.dto.response.guest.AnswerPracticeResponse;
import com.echill.dto.response.guest.QuestionPracticeResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.dto.response.guest.TestSectionPracticeResponse;
import com.echill.service.evaluation.AnswerEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TestEvaluationService {

    private final AnswerEvaluator answerEvaluator;

    public EvaluationContext evaluateWithSnapshot(TestPracticeResponse snapshotTest, Map<Long, Long> userAnswersRaw) {
        if (snapshotTest == null || snapshotTest.getSections() == null || snapshotTest.getSections().isEmpty()) {
            return new EvaluationContext(0, 0, Collections.emptyMap());
        }

        int totalQuestions = 0;
        int correctCount = 0;

        Set<Long> correctAnsIds = new HashSet<>(8);

        Map<Long, TagStats> tagStatsMap = new HashMap<>();

        for (TestSectionPracticeResponse section : snapshotTest.getSections()) {
            if (section.getQuestions() == null) continue;

            for (QuestionPracticeResponse question : section.getQuestions()) {
                totalQuestions++;

                Long currentTagId = question.getTagId();

                TagStats stats = null;
                if (currentTagId != null) {
                    stats = tagStatsMap.computeIfAbsent(currentTagId, k -> new TagStats());
                    stats.total++;
                }

                correctAnsIds.clear();

                if (question.getAnswers() != null) {
                    for (AnswerPracticeResponse ans : question.getAnswers()) {
                        if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                            correctAnsIds.add(ans.getId());
                        }
                    }
                }

                Long userSelected = userAnswersRaw != null ? userAnswersRaw.get(question.getId()) : null;

                if (answerEvaluator.isCorrect(userSelected, correctAnsIds)) {
                    correctCount++;
                    if (stats != null) {
                        stats.correct++;
                    }
                }
            }
        }

        Map<Long, Double> finalTagScores = new HashMap<>((int) (tagStatsMap.size() / 0.75f) + 1);

        for (Map.Entry<Long, TagStats> entry : tagStatsMap.entrySet()) {
            TagStats stats = entry.getValue();

            double percentage = (stats.total == 0)
                    ? 0.0
                    : Math.round(((double) stats.correct / stats.total) * 10000.0) / 100.0;

            finalTagScores.put(entry.getKey(), percentage);
        }

        return new EvaluationContext(correctCount, totalQuestions, Collections.unmodifiableMap(finalTagScores));
    }

    private static class TagStats {
        int total = 0;
        int correct = 0;
    }

    public record EvaluationContext(int correctCount, int totalQuestions, Map<Long, Double> tagProficiencyScores) {}
}