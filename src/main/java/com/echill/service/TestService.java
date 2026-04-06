package com.echill.service;

import com.echill.dto.request.TestRequest;
import com.echill.dto.response.TestResponse;
import com.echill.entity.Question;
import com.echill.entity.Tag;
import com.echill.entity.Test;
import com.echill.entity.TestSet;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.TestRepository;
import com.echill.repository.TestSetRepository;
import com.echill.mapper.TestMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestService {
    TestRepository testRepository;
    TestSetRepository testSetRepository;
    ExcelService excelService;
    TestMapper testMapper;

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
}