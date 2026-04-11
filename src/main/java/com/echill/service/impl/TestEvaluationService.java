package com.echill.service.impl;

import com.echill.repository.AnswerRepository;
import com.echill.repository.QuestionRepository;
import com.echill.repository.projection.CorrectAnswerProjection;
import com.echill.service.evaluation.AnswerEvaluator;
import com.echill.service.evaluation.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestEvaluationService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AnswerEvaluator answerEvaluator;
    private final ScoreCalculator scoreCalculator;

    public EvaluationContext evaluate(Long testId, Map<Long, Long> userAnswersRaw, boolean isLate) {
        Integer totalQuestions = questionRepository.countTotalQuestionsByTestId(testId);

        // Trả về Set<Long> thay vì Long để bao sân cả Multi-choice
        Map<Long, Set<Long>> correctAnswersMap = fetchCorrectAnswers(testId);

        int correctCount = 0;

        // TẠO THÊM MAP NÀY ĐỂ LƯU TRẠNG THÁI ĐÚNG/SAI CỦA TỪNG CÂU
        Map<Long, Boolean> correctnessMap = new HashMap<>();

        for (Long questionId : correctAnswersMap.keySet()) {
            Long userSelected = userAnswersRaw.get(questionId);
            // Chấm điểm từng câu
            boolean isCorrect = answerEvaluator.isCorrect(userSelected, correctAnswersMap.get(questionId), isLate);
            // Lưu lại kết quả câu này vào Map
            correctnessMap.put(questionId, isCorrect);

            if (isCorrect) {
                correctCount++;
            }
        }

        double finalScore = scoreCalculator.calculate(correctCount, totalQuestions, isLate);

        return new EvaluationContext(correctCount, totalQuestions, finalScore, correctAnswersMap.keySet(), correctnessMap);
    }

    private Map<Long, Set<Long>> fetchCorrectAnswers(Long testId) {
        List<CorrectAnswerProjection> projections = answerRepository.findCorrectAnswersByTestId(testId);
        return projections.stream().collect(Collectors.groupingBy(
                CorrectAnswerProjection::getQuestionId,
                Collectors.mapping(CorrectAnswerProjection::getAnswerId, Collectors.toSet())
        ));
    }

    // Tạo record/class nội bộ để return multiple values
    public record EvaluationContext(int correctCount, int totalQuestions, double finalScore, Set<Long> allQuestionIds, Map<Long, Boolean> correctnessMap) {}
}
