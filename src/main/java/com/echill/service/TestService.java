package com.echill.service;

import com.echill.dto.request.QuestionUpdateRequest;
import com.echill.dto.request.TestRequest;
import com.echill.dto.request.TestUpdateRequest;
import com.echill.dto.response.QuestionResponse;
import com.echill.dto.response.TestResponse;
import com.echill.entity.*;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.AnswerMapper;
import com.echill.mapper.QuestionMapper;
import com.echill.repository.*;
import com.echill.mapper.TestMapper;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestService {
    TestRepository testRepository;
    TestSetRepository testSetRepository;
    QuestionRepository questionRepository;
    TagRepository tagRepository;
    AnswerRepository answerRepository;
    ExcelService excelService;
    TestMapper testMapper;
    QuestionMapper questionMapper;
    AnswerMapper answerMapper;

    @Transactional
    public TestResponse createTestWithExcel(TestRequest request, MultipartFile file) {
        log.info("Creating test: {}", request.getTitle());
        if (file == null || file.isEmpty()) {
            throw new AppException(TeacherErrorEnum.FILE_EMPTY);
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.endsWith(".xlsx")) {
            throw new AppException(TeacherErrorEnum.INVALID_FILE_TYPE);
        }

        TestSet testSet = testSetRepository.findById(request.getTestSetId())
                .orElseThrow(() -> {
                    log.error("TestSet not found with id: {}", request.getTestSetId());
                    return new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND);
                });

        if (request.getDurationMinutes() <= 0) {
            throw new AppException(TeacherErrorEnum.INVALID_DURATION);
        }

        if (request.getPassScore() < 0 || request.getPassScore() > 100) {
            throw new AppException(TeacherErrorEnum.INVALID_PASS_SCORE);
        }

        Test test = Test.builder()
                .title(request.getTitle())
                .durationMinutes(request.getDurationMinutes())
                .passScore(request.getPassScore())
                .testSet(testSet)
                .build();

        List<Question> questions = excelService.parseExcelToQuestions(file);
        log.info("Parsed {} questions from Excel", questions.size());

        for (Question question : questions) {
            test.addQuestion(question);
        }

        return testMapper.toResponse(testRepository.save(test));
    }

    public List<TestResponse> getTestsByTestSetId(Long testSetId) {
        return testMapper.toResponseList(testRepository.findByTestSetId(testSetId));
    }

    @Transactional
    public void deleteTest(Long testId) {
        if (!testRepository.existsById(testId)) {
            throw new AppException(TeacherErrorEnum.TEST_NOT_FOUND);
        }
        testRepository.deleteById(testId);
        log.info("Deleting test with id: {}", testId);
    }

    public TestResponse getTestById(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_NOT_FOUND));
        SecurityUtils.validateOwnership(test.getTestSet().getUser().getId());
        return testMapper.toResponse(test);
    }

    @Transactional
    public TestResponse updateTestInfo(Long id, TestUpdateRequest request) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_NOT_FOUND));

        SecurityUtils.validateOwnership(test.getTestSet().getUser().getId());

        testMapper.updateTest(test, request);

        return testMapper.toResponse(testRepository.save(test));
    }

    @Transactional
    public QuestionResponse updateQuestion(Long questionId, QuestionUpdateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.QUESTION_NOT_FOUND));

        SecurityUtils.validateOwnership(question.getTest().getTestSet().getUser().getId());

        // Update basic question info
        questionMapper.updateQuestion(question, request);

        // Update Tag
        if (request.getTagName() != null && !request.getTagName().isBlank()) {
            String normalizedTagName = request.getTagName().trim().toLowerCase();
            if (question.getTag() == null || !normalizedTagName.equals(question.getTag().getName())) {
                Tag tag = tagRepository.findByName(normalizedTagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(normalizedTagName).build()));
                question.setTag(tag);
            }
        } else {
            question.setTag(null);
        }

        // Update Answers: cập nhật theo ID nếu có, tạo mới nếu không
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            Map<Long, Answer> existingAnswerMap = question.getAnswers().stream()
                    .collect(Collectors.toMap(Answer::getId, a -> a));

            request.getAnswers().forEach(ansReq -> {
                if (ansReq.getId() != null && existingAnswerMap.containsKey(ansReq.getId())) {
                    // Cập nhật đáp án hiện tại theo ID
                    answerMapper.updateAnswer(existingAnswerMap.get(ansReq.getId()), ansReq);
                } else {
                    // Tạo mới đáp án nếu không có ID hoặc ID không tồn tại
                    Answer newAnswer = Answer.builder()
                            .content(ansReq.getContent())
                            .isCorrect(ansReq.getIsCorrect())
                            .question(question)
                            .build();
                    question.addAnswer(newAnswer);
                }
            });
        }

        return questionMapper.toResponse(questionRepository.save(question));
    }
}