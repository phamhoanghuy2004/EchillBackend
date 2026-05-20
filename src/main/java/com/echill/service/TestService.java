package com.echill.service;

import com.echill.constant.AppConstants;
import com.echill.dto.exel.ExcelQuestionDto;
import com.echill.dto.request.*;
import com.echill.dto.response.QuestionResponse;
import com.echill.dto.response.SubmitTestResponse;
import com.echill.dto.response.TestResponse;
import com.echill.dto.response.TestResultHistoryResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.dto.response.guest.TestReviewDetailResponse;
import com.echill.dto.response.guest.TestSectionPracticeResponse;
import com.echill.entity.*;
import com.echill.entity.enums.*;
import com.echill.event.QuizPassedEvent;
import com.echill.event.TestEvaluatedEvent;
import com.echill.event.TestUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.*;
import com.echill.policy.SubmissionPolicy;
import com.echill.repository.*;
import com.echill.service.evaluation.ScoreCalculator;
import com.echill.service.impl.TestEvaluationService;
import com.echill.service.persistence.TestPersistService;
import com.echill.util.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidCreator;
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
    TestResultMapper testResultMapper;
    ObjectMapper objectMapper;
    ScoreCalculator scoreCalculator;
    TransactionRepository transactionRepository;
    EnrollmentRepository enrollmentRepository;

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

        log.info("Get test: {}", test);

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

    public TestPracticeResponse getRandomTestForPractice(Long testSetId) {
        return processTestPractice(testSetId, null, null);
    }

    public TestPracticeResponse getSpecificTestForPractice(Long testSetId, Long testId, List<Long> selectedPartIds) {
        return processTestPractice(testSetId, testId, selectedPartIds);
    }

    // =======================================================================
    // 🛠️ CÁC HÀM PRIVATE DÙNG CHUNG CHO GET TEST PRACTICE (HELPER METHODS)
    // =======================================================================

    private TestPracticeResponse processTestPractice(Long testSetId, Long requestedTestId, List<Long> selectedPartIds) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Optional<TestSession> activeSessionOpt = getActiveSession(currentUserId, testSetId);

        TestSession activeSession;
        TestPracticeResponse clientResponse;

        if (activeSessionOpt.isPresent()) {
            activeSession = activeSessionOpt.get();

            if (requestedTestId != null && !activeSession.getTest().getId().equals(requestedTestId)) {
                String testTitle = activeSession.getTest().getTitle();
                String testSetTitle = activeSession.getTest().getTestSet().getTitle();

                String customMessage = String.format(
                        "Bạn đang có bài làm dở: [%s] thuộc bộ [%s]. Vui lòng nộp bài trước khi sang bài mới!",
                        testTitle, testSetTitle
                );

                throw new AppException(StudentErrorEnum.HAS_ACTIVE_SESSION_OTHER_TEST, customMessage);
            }

            clientResponse = parseSnapshotSafe(activeSession);
        } else {
            Long selectedTestId = (requestedTestId != null)
                    ? validateAndGetSpecificTestId(requestedTestId, testSetId)
                    : getRandomAvailableTestId(currentUserId, testSetId);

            TestPracticeResponse fullResponse = self.getCachedTestPractice(selectedTestId);

            clientResponse = prepareSafeResponse(fullResponse, selectedPartIds);

            try {
                activeSession = self.createSessionAndProcessAccess(currentUserId, testSetId, selectedTestId, clientResponse);
            } catch (DataIntegrityViolationException e) {
                log.warn("Race condition detected for user {}. Fetching existing session.", currentUserId);

                activeSession = getActiveSession(currentUserId, testSetId)
                        .orElseThrow(() -> new AppException(ErrorEnum.UNCATEGORIZED));

                clientResponse = parseSnapshotSafe(activeSession);
            }
        }

        return finalizeClientResponse(activeSession, clientResponse);
    }

    private Optional<TestSession> getActiveSession(Long userId, Long testSetId) {
        return testSessionRepository.findFirstByStudentIdAndTestSetIdAndStatusOrderByCreatedAtDesc(
                userId, testSetId, TestSessionStatus.IN_PROGRESS
        );
    }

    private Long getRandomAvailableTestId(Long userId, Long testSetId) {
        List<Long> allTestIds = testRepository.findTestIdsByTestSetId(testSetId);
        if (allTestIds.isEmpty()) {
            throw new AppException(StudentErrorEnum.TEST_NOT_FOUND);
        }

        List<Long> takenTestIds = testResultRepository.findTakenTestIds(userId, testSetId);
        Set<Long> takenSet = new java.util.HashSet<>(takenTestIds);

        List<Long> availableTestIds = allTestIds.stream()
                .filter(id -> !takenSet.contains(id))
                .toList();

        if (availableTestIds.isEmpty()) {
            availableTestIds = allTestIds;
        }

        return availableTestIds.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(availableTestIds.size()));
    }

    private Long validateAndGetSpecificTestId(Long testId, Long testSetId) {
        boolean belongsToSet = testRepository.existsByIdAndTestSetId(testId, testSetId);
        if (!belongsToSet) {
            throw new AppException(StudentErrorEnum.TEST_NOT_FOUND);
        }
        return testId;
    }

    // =======================================================================
    // 💳 PAYMENT & SESSION CREATION (NẰM TRONG TestPracticeService)
    // =======================================================================

    @Transactional(rollbackFor = Exception.class)
    public TestSession createSessionAndProcessAccess(Long userId, Long testSetId, Long selectedTestId, TestPracticeResponse preparedResponse) {

        authorizeAndProcessPayment(userId, testSetId, preparedResponse);

        Test testProxy = testRepository.getReferenceById(selectedTestId);
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(preparedResponse);
        } catch (Exception e) {
            log.error("Lỗi Serialize JSON khi tạo Session cho User: {}", userId, e);
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }

        String lockKey = "USER_" + userId + "_TESTSET_" + testSetId;
        long duration = preparedResponse.getDurationMinutes() != null ? preparedResponse.getDurationMinutes() : 120L;

        TestSession newSession = TestSession.builder()
                .studentId(userId)
                .test(testProxy)
                .testSetId(testSetId)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusMinutes(duration))
                .status(TestSessionStatus.IN_PROGRESS)
                .activeLock(lockKey)
                .testSnapshot(snapshotJson)
                .build();

        return testSessionRepository.saveAndFlush(newSession);
    }
    private void authorizeAndProcessPayment(Long userId, Long testSetId, TestPracticeResponse testInfo) {
        if (Boolean.TRUE.equals(testInfo.getIsPublic())) {
            return;
        }

        if (TestType.PRACTICE.equals(testInfo.getType())) {
            Long courseId = testSetRepository.findCourseIdByTestSetId(testSetId)
                    .orElseThrow(() -> new AppException(ErrorEnum.UNCATEGORIZED, "Đề Practice không được liên kết với Khóa học nào!"));

            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(userId, courseId);

            if (!isEnrolled) {
                throw new AppException(StudentErrorEnum.COURSE_LOCKED, "Bạn phải sở hữu khóa học để làm đề thi này!");
            }

            return;
        }

        processCoinDeduction(userId, testInfo.getTitle());
    }

    private void processCoinDeduction(Long userId, String testTitle) {
        User user = userRepository.findByIdForPaymentLock(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        if (user.getCurrentCoin() < AppConstants.PRIVATE_TEST_FEE) {
            throw new AppException(StudentErrorEnum.NOT_ENOUGH_COIN);
        }

        Long newBalance = user.getCurrentCoin() - AppConstants.PRIVATE_TEST_FEE;
        user.setCurrentCoin(newBalance);

        Transaction transaction = Transaction.builder()
                .transactionCode("PAY-" + TsidCreator.getTsid().toString())
                .totalCoinsChanged(-AppConstants.PRIVATE_TEST_FEE)
                .totalAmount(null)
                .description("Thanh toán phí mở khóa đề thi: " + testTitle)
                .status(TransactionStatus.SUCCESS)
                .type(TransactionType.TEST_PAYMENT)
                .user(user)
                .balanceAfter(newBalance)
                .build();

        transactionRepository.save(transaction);
    }

    private TestPracticeResponse prepareSafeResponse(TestPracticeResponse fullResponse, List<Long> selectedPartIds) {
        TestPracticeResponse safeResponse = testPracticeMapper.clonePracticeResponse(fullResponse);

        if (selectedPartIds != null && !selectedPartIds.isEmpty()) {
            List<TestSectionPracticeResponse> filteredSections = safeResponse.getSections().stream()
                    .filter(section -> selectedPartIds.contains(section.getId()))
                    .toList();
            safeResponse.setSections(filteredSections);
        }

        return safeResponse;
    }

    private TestPracticeResponse finalizeClientResponse(TestSession activeSession, TestPracticeResponse clientResponse) {
        if (activeSession.getEndTime().isBefore(LocalDateTime.now())) {
            throw new AppException(StudentErrorEnum.SESSION_EXPIRED_MUST_SUBMIT);
        }

        cleanUpAnswersForClient(clientResponse);
        clientResponse.setSessionId(activeSession.getId());
        return clientResponse;
    }

    private TestPracticeResponse parseSnapshotSafe(TestSession session) {
        try {
            return objectMapper.readValue(session.getTestSnapshot(), TestPracticeResponse.class);
        } catch (Exception e) {
            log.error("Lỗi Parse JSON bài thi. SessionId: {}", session.getId(), e);
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }
    }

    private void cleanUpAnswersForClient(TestPracticeResponse response) {
        if (response.getSections() != null) {
            response.getSections().forEach(section -> {
                if (section.getQuestions() != null) {
                    section.getQuestions().forEach(question -> {
                        question.setExplanation(null);
                        if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                            question.getAnswers().forEach(ans -> ans.setIsCorrect(null));
                            java.util.Collections.shuffle(question.getAnswers());
                        }
                    });
                }
            });
        }
    }

    @Cacheable(cacheNames = "testPractice", key = "#testId", sync = true)
    @Transactional(readOnly = true)
    public TestPracticeResponse getCachedTestPractice(Long testId) {
        Test test = testRepository.findByIdWithTestSet(testId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.TEST_NOT_FOUND));

        return testPracticeMapper.toPracticeResponse(test);
    }

    public SubmitTestResponse submitTest(SubmitTestRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Map<Long, Long> submittedAnswers = request.getAnswers() != null ? request.getAnswers() : Map.of();

        submissionPolicy.validatePayload(submittedAnswers);

        TestSession session = testSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(StudentErrorEnum.SESSION_NOT_FOUND));

        validateOwnership(session, currentUserId);

        if (session.getStatus() == TestSessionStatus.COMPLETED) {
            TestResult existingResult = testResultRepository.findBySessionId(session.getId())
                    .orElseThrow(() -> new AppException(StudentErrorEnum.ALREADY_SUBMITTED));
            return buildResponse(existingResult);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isLate = submissionPolicy.isLate(session.getEndTime(), now);
        int timeTaken = Math.max(0, (int) ChronoUnit.SECONDS.between(session.getStartTime(), now));

        TestPracticeResponse snapshotTest;
        String userAnswersJson;
        try {
            snapshotTest = objectMapper.readValue(session.getTestSnapshot(), TestPracticeResponse.class);
            userAnswersJson = objectMapper.writeValueAsString(submittedAnswers);
        } catch (Exception e) {
            log.error("Lỗi Parse JSON bài thi. SessionId: {}", session.getId(), e);
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }

        var evalCtx = evaluationService.evaluateWithSnapshot(snapshotTest, submittedAnswers);

        int finalCorrectAnswers = isLate ? 0 : evalCtx.correctCount();
        double finalScore = scoreCalculator.calculate(finalCorrectAnswers, evalCtx.totalQuestions(), isLate);

        TestResult testResult = TestResult.builder()
                .student(userRepository.getReferenceById(currentUserId))
                .test(session.getTest())
                .sessionId(session.getId())
                .timeTakenSeconds(timeTaken)
                .isLate(isLate)
                .totalScore(finalScore)
                .correctAnswers(finalCorrectAnswers)
                .totalQuestions(evalCtx.totalQuestions())
                .testSnapshot(session.getTestSnapshot())
                .userAnswersSnapshot(userAnswersJson)
                .build();

        testResult.evaluateResult(snapshotTest.getPassScore());

        testResult = self.saveResultAndCloseSession(testResult, session.getId(), session.getTestSetId(), evalCtx.tagLevelScores(), snapshotTest.getType());

        return buildResponse(testResult);
    }

    @Transactional
    public TestResult saveResultAndCloseSession(TestResult result, Long sessionId, Long testSetId, Map<Long, Integer> tagLevelScores, TestType testType) {
        int updatedRows = testSessionRepository.updateStatusConditionally(
                sessionId, TestSessionStatus.COMPLETED, TestSessionStatus.IN_PROGRESS
        );

        if (updatedRows == 0) {
            return testResultRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new AppException(StudentErrorEnum.ALREADY_SUBMITTED));
        }

        TestResult savedResult = testResultRepository.save(result);

        if (Boolean.TRUE.equals(savedResult.getIsPassed())) {
            log.info("📢 Bắn event QuizPassed cho Student: {} - TestSet: {}",
                    savedResult.getStudent().getId(), testSetId);

            eventPublisher.publishEvent(new QuizPassedEvent(
                    savedResult.getStudent().getId(),
                    testSetId
            ));
        }

        // 🟢 FIX TÊN BIẾN: Sửa tagScores thành tagLevelScores
        if (tagLevelScores != null && !tagLevelScores.isEmpty()) {
            log.info("📢 Bắn event TestEvaluated cho Student: {} với {} tags",
                    savedResult.getStudent().getId(), tagLevelScores.size());

            eventPublisher.publishEvent(new TestEvaluatedEvent(
                    savedResult.getStudent().getId(),
                    tagLevelScores,
                    testType
            ));
        }

        return savedResult;
    }

    private void validateOwnership(TestSession session, Long currentUserId) {
        if (!session.getStudentId().equals(currentUserId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
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

    public TestReviewDetailResponse getTestReviewDetails(Long resultId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return self.getCachedReviewDetails(resultId, currentUserId);
    }

    @Cacheable(value = "testReviewCache", key = "#resultId + '-' + #currentUserId")
    @Transactional(readOnly = true)
    public TestReviewDetailResponse getCachedReviewDetails(Long resultId, Long currentUserId) {

        TestResult testResult = testResultRepository.findByIdWithDetails(resultId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.TEST_RESULT_NOT_FOUND));

        if (!testResult.getStudent().getId().equals(currentUserId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }

        TestPracticeResponse fullTestWithAnswers;
        Map<Long, Long> rawAnswers;

        try {
            fullTestWithAnswers = objectMapper.readValue(
                    testResult.getTestSnapshot(),
                    TestPracticeResponse.class
            );

            rawAnswers = objectMapper.readValue(
                    testResult.getUserAnswersSnapshot(),
                    new TypeReference<Map<Long, Long>>() {}
            );

        } catch (Exception e) {
            log.error("Lỗi phục hồi JSON snapshot cho resultId: {}", resultId, e);
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }

        TestResultHistoryResponse summary = testResultMapper.toHistoryResponse(testResult);

        return TestReviewDetailResponse.builder()
                .summary(summary)
                .testData(fullTestWithAnswers)
                .userAnswers(rawAnswers)
                .build();
    }
}