package com.echill.service.impl;

import com.echill.dto.response.guest.AnswerPracticeResponse;
import com.echill.dto.response.guest.QuestionPracticeResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.dto.response.guest.TestSectionPracticeResponse;
import com.echill.service.evaluation.AnswerEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
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

        // 1. Gom nhóm thống kê theo từng Tag (Bao gồm Tổng câu, Số câu đúng, và Tổng độ khó)
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

                    // 🟢 Lấy độ khó của câu hỏi (Mặc định là 3 nếu Admin quên nhập)
                    int diff = (question.getDifficultyLevel() != null) ? question.getDifficultyLevel() : 3;
                    stats.sumDifficulty += diff;
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

        // 2. Thuật toán Batch Tag Scoring: Tính Effective Level cho từng Tag
        Map<Long, Integer> finalTagLevelScores = new HashMap<>((int) (tagStatsMap.size() / 0.75f) + 1);

        for (Map.Entry<Long, TagStats> entry : tagStatsMap.entrySet()) {
            TagStats stats = entry.getValue();
            Long tagId = entry.getKey();

            if (stats.total == 0) continue;

            double accuracy = (double) stats.correct / stats.total;
            int avgDifficulty = (int) Math.round((double) stats.sumDifficulty / stats.total);

            log.info("tagId: {}, avgDifficulty: {}", tagId, avgDifficulty);
            log.info("tagId: {}, accuracy: {}", tagId, accuracy);

            int effectiveLevel;
            if (accuracy >= 0.8) { // Đúng >= 80%: Trúng tủ, vượt level
                effectiveLevel = Math.min(5, avgDifficulty + 1);
            } else if (accuracy >= 0.4) { // Đúng từ 40% - 79%: Đạt chuẩn
                effectiveLevel = avgDifficulty;
            } else { // Dưới 40%: Quá sức
                effectiveLevel = Math.max(1, avgDifficulty - 1);
            }

            log.debug("Tag ID: {} | Qty: {} | Acc: {}% | AvgDiff: {} -> Effective Level: {}",
                    tagId, stats.total, Math.round(accuracy * 100), avgDifficulty, effectiveLevel);

            finalTagLevelScores.put(tagId, effectiveLevel);
        }

        // Trả về Map<Long, Integer> thay vì Map<Long, Double>
        return new EvaluationContext(correctCount, totalQuestions, Collections.unmodifiableMap(finalTagLevelScores));
    }

    /**
     * Nâng cấp TagStats để chứa thêm sumDifficulty
     */
    private static class TagStats {
        int total = 0;
        int correct = 0;
        int sumDifficulty = 0; // 🟢 Thêm biến này
    }

    /**
     * Đổi tagProficiencyScores từ Double sang Integer (tagLevelScores)
     */
    public record EvaluationContext(int correctCount, int totalQuestions, Map<Long, Integer> tagLevelScores) {}
}