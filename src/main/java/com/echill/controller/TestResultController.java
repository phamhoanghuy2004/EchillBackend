package com.echill.controller;

import com.echill.dto.request.TestResultHistoryRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.TestResultHistoryDto;
import com.echill.service.TestResultService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-results")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestResultController {
    TestResultService testResultService;

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResponse<TestResultHistoryDto>> getMyHistory(
            @Valid @ModelAttribute TestResultHistoryRequest request
    ) {
        PageResponse<TestResultHistoryDto> resultData = testResultService.getMyTestHistory(request);

        return ApiResponse.<PageResponse<TestResultHistoryDto>>builder()
                .data(resultData)
                .message("Lấy lịch sử làm bài thành công")
                .build();
    }
}
