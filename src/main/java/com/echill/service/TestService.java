package com.echill.service;

import com.echill.constant.AppConstants;
import com.echill.dto.exel.ExcelQuestionDto;
import com.echill.dto.request.*;
import com.echill.dto.response.QuestionResponse;
import com.echill.dto.response.SubmitTestResponse;
import com.echill.dto.response.TestResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.entity.*;
import com.echill.entity.enums.AnswerOption;
import com.echill.entity.enums.TestSessionStatus;
import com.echill.event.TestUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.AnswerMapper;
import com.echill.mapper.QuestionMapper;
import com.echill.mapper.TestPracticeMapper;
import com.echill.policy.SubmissionPolicy;
import com.echill.repository.*;
import com.echill.mapper.TestMapper;
import com.echill.service.impl.TestEvaluationService;
import com.echill.service.persistence.TestPersistService;
import com.echill.service.persistence.TestResultPersistenceService;
import com.echill.util.SecurityUtils;
import org.springframework.cache.annotation.Cacheable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    ExcelService excelService;
    TestMapper testMapper;
    QuestionMapper questionMapper;
    AnswerMapper answerMapper;
    TestPersistService testPersistService;
    TestResultRepository testResultRepository;
    TestPracticeMapper testPracticeMapper;
    ApplicationEventPublisher eventPublisher;
    TestSessionRepository testSessionRepository;
    UserRepository userRepository;
    SubmissionPolicy submissionPolicy;
    TestEvaluationService evaluationService;
    TestResultPersistenceService persistenceService;

    @Lazy
    @Autowired
    @NonFinal
    TestService self;

    @Value("${app.test.max-questions-per-file:2000}")
    @NonFinal
    int maxQuestionsPerFile;

    static String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final int GRACE_PERIOD_SECONDS = 60;

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

        eventPublisher.publishEvent(new TestUpdatedEvent(test.getId()));

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
                existingAns.setQuestion(null);
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

        Long testId = question.getSection().getTest().getId();
        eventPublisher.publishEvent(new TestUpdatedEvent(testId));

        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public TestPracticeResponse getRandomTestForPractice(Long testSetId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        userRepository.findByIdWithLock(currentUserId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Optional<TestSession> activeSessionOpt = testSessionRepository
                .findFirstByStudentIdAndTestSetIdAndStatusOrderByCreatedAtDesc(
                        currentUserId, testSetId, TestSessionStatus.IN_PROGRESS
                );

        Long selectedTestId;
        Long activeSessionId;
        TestPracticeResponse cachedResponse; // Khai báo 1 lần ở ngoài

        if (activeSessionOpt.isPresent()) {
            TestSession activeSession = activeSessionOpt.get();

            if (activeSession.getEndTime().isAfter(LocalDateTime.now())) {
                selectedTestId = activeSession.getTest().getId(); // Lấy ID qua Proxy, không tốn query
                activeSessionId = activeSession.getId();
                // Lấy cache cho session đang làm dở
                cachedResponse = self.getCachedTestPractice(selectedTestId);
            } else {
                throw new AppException(StudentErrorEnum.SESSION_EXPIRED_MUST_SUBMIT);
            }
        } else {
            long attempts = testResultRepository.countByStudentAndTestSet(currentUserId, testSetId);
            if (attempts >= AppConstants.MAX_TEST_ATTEMPTS) {
                throw new AppException(StudentErrorEnum.MAX_ATTEMPT_REACHED);
            }

            List<Long> allTestIds = testRepository.findTestIdsByTestSetId(testSetId);
            if (allTestIds.isEmpty()) {
                throw new AppException(StudentErrorEnum.TEST_NOT_FOUND);
            }

            List<Long> takenTestIds = testResultRepository.findTakenTestIds(currentUserId, testSetId);
            Set<Long> takenSet = new java.util.HashSet<>(takenTestIds);
            List<Long> availableTestIds = allTestIds.stream()
                    .filter(id -> !takenSet.contains(id))
                    .toList();

            if (availableTestIds.isEmpty()) {
                availableTestIds = allTestIds;
            }

            int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(availableTestIds.size());
            selectedTestId = availableTestIds.get(randomIndex);

            // Chỉ gọi cache 1 lần duy nhất cho test mới
            cachedResponse = self.getCachedTestPractice(selectedTestId);
            Test testProxy = testRepository.getReferenceById(selectedTestId); // Proxy siêu nhẹ

            TestSession newSession = TestSession.builder()
                    .studentId(currentUserId)
                    .test(testProxy)
                    .testSetId(testSetId)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusMinutes(cachedResponse.getDurationMinutes()))
                    .status(TestSessionStatus.IN_PROGRESS)
                    .build();

            // Đảm bảo bạn có Unique Index dưới DB để chặn double-click ở dòng này
            newSession = testSessionRepository.save(newSession);

            activeSessionId = newSession.getId();
        }

        // Đã có cachedResponse (dù là session cũ hay mới tạo), clone luôn
        TestPracticeResponse safeResponse = testPracticeMapper.clonePracticeResponse(cachedResponse);

        // Xáo trộn đáp án an toàn trên Object đã clone
        safeResponse.getSections().forEach(section ->
                section.getQuestions().forEach(question -> {
                    if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                        java.util.Collections.shuffle(question.getAnswers());
                    }
                })
        );

        safeResponse.setSessionId(activeSessionId);
        return safeResponse;
    }

    @Cacheable(cacheNames = "testPractice", key = "#testId", sync = true)
    @Transactional(readOnly = true)
    public TestPracticeResponse getCachedTestPractice(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.TEST_NOT_FOUND));

        return testPracticeMapper.toPracticeResponse(test);
    }

    @Transactional
    public SubmitTestResponse submitTest(SubmitTestRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Map<Long, Long> submittedAnswers = request.getAnswers() != null ? request.getAnswers() : Map.of();

        // 1. Validate Policy
        submissionPolicy.validatePayload(submittedAnswers);

        // 2. Load and Lock (Chống Race Condition & Tránh query Test dư thừa)
        TestSession session = testSessionRepository.findByIdWithLockAndFetchTest(request.getSessionId())
                .orElseThrow(() -> new AppException(StudentErrorEnum.SESSION_NOT_FOUND));

        validateOwnership(session, currentUserId);

        // 3. Đánh giá thời gian
        LocalDateTime now = LocalDateTime.now();
        boolean isLate = submissionPolicy.isLate(session.getEndTime(), now);
        int timeTaken = (int) ChronoUnit.SECONDS.between(session.getStartTime(), now);

        // 4. Chấm điểm (Delegate to EvaluationService)
        Test test = session.getTest();
        var evalCtx = evaluationService.evaluate(test.getId(), submittedAnswers, isLate);

        // 5. Chuẩn bị Entity
        TestResult testResult = TestResult.builder()
                .student(userRepository.getReferenceById(currentUserId))
                .test(test)
                .timeTakenSeconds(timeTaken)
                .isLate(isLate)
                .totalScore(evalCtx.finalScore())
                .correctAnswers(isLate ? 0 : evalCtx.correctCount())
                .totalQuestions(evalCtx.totalQuestions())
                .build();
        testResult.evaluateResult(test.getPassScore());

        // 6. Lưu Data (Delegate to PersistenceService - Tránh N+1)
        testResult = persistenceService.persistResult(testResult, submittedAnswers, evalCtx.allQuestionIds(), evalCtx.correctnessMap());

        // 7. Đóng Session
        session.setStatus(TestSessionStatus.COMPLETED);
        testSessionRepository.save(session);

        return buildResponse(testResult);
    }

    private void validateOwnership(TestSession session, Long currentUserId) {
        if (!session.getStudentId().equals(currentUserId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }
        if (session.getStatus() == TestSessionStatus.COMPLETED) {
            throw new AppException(StudentErrorEnum.ALREADY_SUBMITTED);
        }
    }

    private SubmitTestResponse buildResponse(TestResult result) {
        boolean isLate = result.getIsLate() != null && result.getIsLate();

        return SubmitTestResponse.builder()
                .resultId(result.getId())
                .score(result.getTotalScore())
                .correctAnswers(result.getCorrectAnswers())
                .totalQuestions(result.getTotalQuestions())
                .isLate(isLate)
                .isPassed(result.getIsPassed())
                .message(isLate ? "Bài nộp quá hạn quy định. Điểm: 0" : "Nộp bài thành công!")
                .build();
    }
}