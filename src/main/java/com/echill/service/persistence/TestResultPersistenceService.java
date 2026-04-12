package com.echill.service.persistence;

import com.echill.entity.Answer;
import com.echill.entity.Question;
import com.echill.entity.TestResult;
import com.echill.entity.UserAnswer;
import com.echill.repository.AnswerRepository;
import com.echill.repository.QuestionRepository;
import com.echill.repository.TestResultRepository;
import com.echill.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestResultPersistenceService {
    private final TestResultRepository testResultRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    // Yêu cầu bắt buộc phải nằm trong 1 Transaction đang có sẵn
    @Transactional(propagation = Propagation.MANDATORY)
    public TestResult persistResult(TestResult result, Map<Long, Long> userAnswersRaw, Set<Long> questionIds, Map<Long, Boolean> correctnessMap) {

        // 1. Lưu TestResult trước để lấy ID
        TestResult savedResult = testResultRepository.save(result);

        // 2. NẾU BÀI TEST KHÔNG CÓ CÂU HỎI (Trường hợp hiếm nhưng cần thủ thế)
        if (questionIds == null || questionIds.isEmpty()) {
            return savedResult;
        }

        // Lấy Question Map
        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds)
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        // 3. Lọc lấy ID đáp án user đã chọn
        Set<Long> submittedAnswerIds = userAnswersRaw.values().stream()
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // 4. CHECK RỖNG TRƯỚC KHI GỌI DATABASE (Chống lỗi SQL IN ())
        Map<Long, Answer> answerMap = new HashMap<>();
        if (!submittedAnswerIds.isEmpty()) {
            answerMap = answerRepository.findAllById(submittedAnswerIds)
                    .stream().collect(Collectors.toMap(Answer::getId, a -> a));
        }

        // Build batch UserAnswers
        List<UserAnswer> userAnswersBatch = new ArrayList<>();
        for (Long qId : questionIds) {
            Long aId = userAnswersRaw.get(qId);
            boolean isCorrect = correctnessMap.getOrDefault(qId, false);

            userAnswersBatch.add(UserAnswer.builder()
                    .testResult(savedResult)
                    .question(questionMap.get(qId))
                    .selectedAnswer(aId != null ? answerMap.get(aId) : null)
                    .isCorrect(isCorrect)
                    .build());
        }

        // Batch Insert
        userAnswerRepository.saveAll(userAnswersBatch);
        return savedResult;
    }
}
