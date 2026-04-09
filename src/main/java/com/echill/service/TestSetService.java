package com.echill.service;

import com.echill.dto.request.TestSetRequest;
import com.echill.dto.response.TestSetDetailWithHistoryResponse;
import com.echill.dto.request.TestSetUpdateRequest;
import com.echill.dto.response.TestSetResponse;
import com.echill.entity.Lesson;
import com.echill.entity.TestResult;
import com.echill.entity.TestSet;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.LessonRepository;
import com.echill.repository.TestResultRepository;
import com.echill.repository.TestSetRepository;
import com.echill.repository.UserRepository;
import com.echill.mapper.TestSetMapper;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestSetService {
    TestSetRepository testSetRepository;
    LessonRepository lessonRepository;
    UserRepository userRepository;
    TestSetMapper testSetMapper;
    TestResultRepository testResultRepository;

    @Transactional
    public TestSetResponse createTestSet(TestSetRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        // Validate: Lesson already has a TestSet
        if (testSetRepository.findByLessonId(request.getLessonId()).isPresent()) {
            throw new AppException(TeacherErrorEnum.TEST_SET_EXISTED);
        }

        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        TestSet testSet = testSetMapper.toEntity(request);
        testSet.setLesson(lesson);
        testSet.setUser(user);
        if (testSet.getIsPublic() == null) testSet.setIsPublic(true);

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

    @Transactional
    public TestSetResponse updateTestSet(Long id, TestSetUpdateRequest request) {
        TestSet testSet = testSetRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));

        SecurityUtils.validateOwnership(testSet.getUser().getId());

        testSetMapper.updateTestSet(testSet, request);

        return testSetMapper.toResponse(testSetRepository.save(testSet));
    }
}
