package com.echill.service;

import com.echill.dto.request.TestResultHistoryRequest;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.TestResultHistoryDto;
import com.echill.entity.enums.TestType;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.TestResultRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestResultService {
    TestResultRepository testResultRepository;

    @Transactional(readOnly = true)
    public PageResponse<TestResultHistoryDto> getMyTestHistory(TestResultHistoryRequest request) {
        Long studentId = SecurityUtils.getCurrentUserId();

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant startInstant = request.getStartDate() != null ? request.getStartDate().atStartOfDay(vnZone).toInstant() : null;
        Instant endInstant = request.getEndDate() != null ? request.getEndDate().atTime(LocalTime.MAX).atZone(vnZone).toInstant() : null;

        String searchTitle = org.springframework.util.StringUtils.hasText(request.getTestTitle())
                ? "%" + request.getTestTitle() + "%"
                : null;

        Page<TestResultHistoryDto> springPage = testResultRepository.getMyHistoryOptimized(
                studentId,
                request.getTestId(),
                searchTitle,
                startInstant,
                endInstant,
                request.getPageable()
        );

        return PageResponse.of(springPage);
    }

    @Transactional(readOnly = true)
    public List<TestResultHistoryDto> getRecentFullTestsForEstimation() {
        Long studentId = SecurityUtils.getCurrentUserId();

        log.info("🔍 Đang trích xuất lịch sử 5 bài Full TOEIC gần nhất cho User {}", studentId);

        List<TestResultHistoryDto> recentTests = testResultRepository.findTopRecentFullTests(
                studentId,
                TestType.TOEIC,
                PageRequest.of(0, 5)
        );

        if (recentTests.size() < 5) {
            log.warn("⚠️ User {} mới chỉ làm {}/5 bài Full Test. Yêu cầu làm thêm.", studentId, recentTests.size());
            throw new AppException(ErrorEnum.NOT_ENOUGH_FULL_TESTS);
        }

        return recentTests;
    }
}
