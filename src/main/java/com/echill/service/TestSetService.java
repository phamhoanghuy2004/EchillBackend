package com.echill.service;

import com.echill.dto.request.TestSetRequest;
import com.echill.dto.request.TestSetSearchRequest;
import com.echill.dto.response.*;
import com.echill.dto.request.TestSetUpdateRequest;
import com.echill.dto.response.learner.TestSetRecommendationResponse;
import com.echill.entity.Lesson;
import com.echill.entity.TestResult;
import com.echill.entity.TestSet;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.*;
import com.echill.mapper.TestSetMapper;
import com.echill.repository.projection.TestQuestionCountProjection;
import com.echill.repository.specification.TestSetSpecification;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestSetService {
    TestSetRepository testSetRepository;
    LessonRepository lessonRepository;
    UserRepository userRepository;
    TestSetMapper testSetMapper;
    TestResultRepository testResultRepository;
    TestRepository testRepository;

    @Lazy
    @Autowired
    @lombok.experimental.NonFinal
    TestSetService self;

    @Transactional
    public TestSetResponse createTestSet(TestSetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.getReferenceById(userId);

        TestSet testSet = testSetMapper.toEntity(request);
        testSet.setUser(user);

        if (testSet.getIsPublic() == null) {
            testSet.setIsPublic(true);
        }

        if (request.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

            if (testSetRepository.findByLessonId(request.getLessonId()).isPresent()) {
                throw new AppException(TeacherErrorEnum.TEST_SET_EXISTED);
            }

            testSet.setLesson(lesson);
        }

        return testSetMapper.toResponse(testSetRepository.save(testSet));
    }

    public TestSetResponse getTestSetByLessonId(Long lessonId) {
        TestSet testSet = testSetRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));
        return testSetMapper.toResponse(testSet);
    }

    @Transactional(readOnly = true)
    public TestSetDetailWithHistoryResponse getTestSetDetailWithHistory(Long testSetId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));

        List<TestResult> userHistory = testResultRepository.findHistoryByStudentAndTestSet(currentUserId, testSetId);

        return testSetMapper.toDetailWithHistory(testSet, userHistory);
    }

    public List<TestSetResponse> getAllTestSets() {
        Long userId = SecurityUtils.getCurrentUserId();
        return testSetMapper.toResponseList(testSetRepository.findAllByUserId(userId));
    }

    @Transactional
    public TestSetResponse updateTestSet(Long id, TestSetUpdateRequest request) {
        TestSet testSet = testSetRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));


        SecurityUtils.validateOwnership(testSet.getUser().getId());

        testSetMapper.updateTestSet(testSet, request);

        return testSetMapper.toResponse(testSetRepository.save(testSet));
    }

    @Transactional(readOnly = true)
    public List<TestSetRecommendationResponse> getNewestTestSetsForCurrentYear() {
        // 1. Lấy năm hiện tại theo múi giờ Việt Nam (Tránh lỗi lệch giờ ở máy chủ)
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        int currentYear = ZonedDateTime.now(zoneId).getYear();

        // 2. Giới hạn chỉ lấy 5 bộ đề mới nhất (Tối ưu Payload)
        int limit = 5;

        return testSetRepository.findRecommendedTestSets(currentYear, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public PageResponse<TestSetResponse> searchTestSets(TestSetSearchRequest request) {

        Specification<TestSet> spec = TestSetSpecification.buildSearch(
                request.getKeyword(),
                request.getYear(),
                request.getType()
        );

        Page<TestSet> testSetPage = testSetRepository.findAll(spec, request.getPageable());

        Page<TestSetResponse> responsePage = testSetPage.map(testSet -> {
            TestSetResponse response = testSetMapper.toResponse(testSet);

            if (Boolean.TRUE.equals(testSet.getIsPublic())) {
                response.setPrice(0);
            } else {
                response.setPrice(10);
            }

            return response;
        });

        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public TestSetDetailResponse getTestSetDetail(Long testSetId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        TestSetCacheDto testSet = self.getCachedTestSet(testSetId);

        List<QuestionCountDto> cachedCounts = self.getCachedQuestionCounts(testSetId);

        Map<Long, Long> questionCountsMap = cachedCounts.stream()
                .collect(Collectors.toMap(QuestionCountDto::testId, QuestionCountDto::totalQuestions));

        Set<Long> takenTestIds = (currentUserId != null)
                ? testResultRepository.findTakenTestIdsByStudentAndTestSet(currentUserId, testSetId)
                : java.util.Collections.emptySet();

        Integer defaultPrice = Boolean.TRUE.equals(testSet.isPublic()) ? 0 : 10;

        List<TestSummaryResponse> testList = testSet.tests().stream().map(test ->
                TestSummaryResponse.builder()
                        .id(test.id())
                        .title(test.title())
                        .durationMinutes(test.durationMinutes())
                        .price(defaultPrice)
                        .totalQuestions(questionCountsMap.getOrDefault(test.id(), 0L))
                        .hasAttempted(takenTestIds.contains(test.id()))
                        .build()
        ).toList();

        return TestSetDetailResponse.builder()
                .id(testSet.id())
                .title(testSet.title())
                .description(testSet.description())
                .isPublic(testSet.isPublic())
                .tests(testList)
                .build();
    }

    // =======================================================================
    // 🛠️ CACHING METHODS (Lưu ở Redis/Memory)
    // =======================================================================

    @Cacheable(cacheNames = "testSetDetails", key = "#testSetId", sync = true)
    @Transactional(readOnly = true)
    public TestSetCacheDto getCachedTestSet(Long testSetId) {
        TestSet testSet = testSetRepository.findByIdWithTests(testSetId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));

        List<TestCacheDto> testDtos = testSet.getTests().stream()
                .map(t -> new TestCacheDto(t.getId(), t.getTitle(), t.getDurationMinutes()))
                .toList();

        return new TestSetCacheDto(
                testSet.getId(),
                testSet.getTitle(),
                testSet.getDescription(),
                testSet.getIsPublic(),
                testDtos
        );
    }

    @Cacheable(cacheNames = "testQuestionCounts", key = "#testSetId", sync = true)
    @Transactional(readOnly = true)
    public List<QuestionCountDto> getCachedQuestionCounts(Long testSetId) {
        List<TestQuestionCountProjection> projections = testRepository.countQuestionsByTestSetId(testSetId);

        // Lưu mảng List vào Redis cực kỳ an toàn
        return projections.stream()
                .map(p -> new QuestionCountDto(p.getTestId(), p.getTotalQuestions()))
                .toList();
    }
}
