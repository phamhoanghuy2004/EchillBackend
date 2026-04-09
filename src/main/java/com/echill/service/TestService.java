package com.echill.service;

import com.echill.dto.exel.ExcelQuestionDto;
import com.echill.dto.request.AnswerRequest;
import com.echill.dto.request.QuestionUpdateRequest;
import com.echill.dto.request.TestRequest;
import com.echill.dto.request.TestUpdateRequest;
import com.echill.dto.response.QuestionResponse;
import com.echill.dto.response.TestResponse;
import com.echill.entity.*;
import com.echill.entity.enums.AnswerOption;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.AnswerMapper;
import com.echill.mapper.QuestionMapper;
import com.echill.repository.*;
import com.echill.mapper.TestMapper;
import com.echill.service.persistence.TestPersistService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
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
    TestPersistService testPersistService;

    @Value("${app.test.max-questions-per-file:2000}")
    @NonFinal
    int maxQuestionsPerFile;

    static String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public TestResponse createTestWithExcel(TestRequest request, MultipartFile file) {
        log.info("Starting create test: [{}] for TestSet ID: {}", request.getTitle(), request.getTestSetId());

        validateRequest(request);
        validateFile(file);

        List<ExcelQuestionDto> parsedData = excelService.parseExcelToDto(file);
        if (parsedData.isEmpty()) {
            throw new AppException(TeacherErrorEnum.FILE_EMPTY);
        }

        validateDtoList(parsedData, request);

        return testPersistService.saveTestTransaction(request, parsedData);
    }

    private void validateDtoList(List<ExcelQuestionDto> dtoList, TestRequest request) {
        if (dtoList.size() > maxQuestionsPerFile) {
            log.error("File for Test [{}] contains {} questions, exceeding limit {}",
                    request.getTitle(), dtoList.size(), maxQuestionsPerFile);
            throw new AppException(TeacherErrorEnum.FILE_TOO_LARGE);
        }

        for (ExcelQuestionDto dto : dtoList) {
            if (dto.getSkillType() == null || dto.getCorrectAnswer() == null) {
                log.error("Test [{}]: Missing Core Data at row {}", request.getTitle(), dto.getRowNumber());
                throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT);
            }

            if (AnswerOption.D.name().equals(dto.getCorrectAnswer()) &&
                    (dto.getOptionD() == null || dto.getOptionD().isBlank())) {

                log.error("Test [{}] | TestSet [{}]: Row {} correct answer is D but option D is null",
                        request.getTitle(), request.getTestSetId(), dto.getRowNumber());
                throw new AppException(TeacherErrorEnum.INVALID_CORRECT_ANSWER);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(TeacherErrorEnum.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        boolean isExcelContent = EXCEL_MIME_TYPE.equals(contentType);
        boolean isExcelExtension = fileName != null && fileName.endsWith(".xlsx");

        if (!isExcelContent || !isExcelExtension) {
            log.warn("Invalid file upload detected. Name: {}, Content-Type: {}", fileName, contentType);
            throw new AppException(TeacherErrorEnum.INVALID_FILE_TYPE);
        }
    }

    private void validateRequest(TestRequest request) {
        if (request.getDurationMinutes() <= 0) {
            throw new AppException(TeacherErrorEnum.INVALID_DURATION);
        }
        if (request.getPassScore() < 0 || request.getPassScore() > 100) {
            throw new AppException(TeacherErrorEnum.INVALID_PASS_SCORE);
        }
    }

    @Transactional
    public void deleteTest(Long testId) {
        if (!testRepository.existsById(testId)) {
            throw new AppException(TeacherErrorEnum.TEST_NOT_FOUND);
        }
        testRepository.deleteById(testId);
        log.info("Deleting test with id: {}", testId);
    }

    @Transactional(readOnly = true)
    public TestResponse getTestById(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_NOT_FOUND));

        SecurityUtils.validateOwnership(test.getTestSet().getUser().getId());

        return testMapper.toResponse(test);
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsByTestSetId(Long testSetId) {

        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));

        SecurityUtils.validateOwnership(testSet.getUser().getId());

        List<Test> tests = testRepository.findByTestSetId(testSetId);
        return testMapper.toResponseList(tests);
    }

    @Transactional
    public TestResponse updateTestInfo(Long id, TestUpdateRequest request) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_NOT_FOUND));

        SecurityUtils.validateOwnership(test.getTestSet().getUser().getId());

        testMapper.updateTest(test, request);

        return testMapper.toResponse(testRepository.save(test));
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionResponse updateQuestion(Long questionId, QuestionUpdateRequest request) {

        Question question = questionRepository.findByIdWithFullRelations(questionId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.QUESTION_NOT_FOUND));

        Long ownerId = question.getSection().getTest().getTestSet().getUser().getId();
        SecurityUtils.validateOwnership(ownerId);

        if (request.getAnswers().size() < 2 || request.getAnswers().size() > 6) {
            throw new AppException(TeacherErrorEnum.INVALID_ANSWER_COUNT);
        }

        long correctCount = 0;
        Set<String> uniqueContents = new HashSet<>();

        for (AnswerRequest ansReq : request.getAnswers()) {
            if (Boolean.TRUE.equals(ansReq.getIsCorrect())) {
                correctCount++;
            }
            if (!uniqueContents.add(ansReq.getContent().trim().toLowerCase())) {
                throw new AppException(TeacherErrorEnum.DUPLICATE_ANSWER_CONTENT);
            }
        }

        if (correctCount != 1) {
            throw new AppException(TeacherErrorEnum.EXACTLY_ONE_CORRECT_ANSWER_REQUIRED);
        }

        questionMapper.updateQuestion(question, request);

        if (request.getTagName() != null && !request.getTagName().isBlank()) {
            String normalizedTagName = request.getTagName().trim().toLowerCase();
            if (question.getTag() == null || !normalizedTagName.equals(question.getTag().getName())) {
                try {
                    Tag tag = tagRepository.findByName(normalizedTagName)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(normalizedTagName).build()));
                    question.setTag(tag);
                } catch (DataIntegrityViolationException e) {
                    Tag existingTag = tagRepository.findByName(normalizedTagName)
                            .orElseThrow(() -> new AppException(TeacherErrorEnum.TAG_CREATION_FAILED));
                    question.setTag(existingTag);
                }
            }
        } else {
            question.setTag(null);
        }

        Set<Long> requestedAnswerIds = request.getAnswers().stream()
                .map(AnswerRequest::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        question.getAnswers().removeIf(existingAns -> {
            boolean shouldRemove = existingAns.getId() != null && !requestedAnswerIds.contains(existingAns.getId());
            if (shouldRemove) {
                existingAns.setQuestion(null); // Bắt buộc set null để Hibernate xóa mồ côi (Orphan) chuẩn
            }
            return shouldRemove;
        });

        Map<Long, Answer> existingAnswerMap = question.getAnswers().stream()
                .collect(Collectors.toMap(Answer::getId, a -> a));

        request.getAnswers().forEach(ansReq -> {
            if (ansReq.getId() != null) {
                if (existingAnswerMap.containsKey(ansReq.getId())) {
                    answerMapper.updateAnswer(existingAnswerMap.get(ansReq.getId()), ansReq);
                } else {
                    throw new AppException(TeacherErrorEnum.INVALID_ANSWER_ID); // Văng lỗi Spoofing
                }
            } else {
                Answer newAnswer = Answer.builder()
                        .content(ansReq.getContent())
                        .isCorrect(ansReq.getIsCorrect())
                        .build();
                question.addAnswer(newAnswer);
            }
        });

        return questionMapper.toResponse(questionRepository.save(question));
    }
}