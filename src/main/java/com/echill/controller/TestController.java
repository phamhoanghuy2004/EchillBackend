package com.echill.controller;

import com.echill.dto.request.TestRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.TestResponse;
import com.echill.service.TestService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestController {
    TestService testService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TestResponse> createTest(
            @RequestPart("data") @Valid TestRequest request,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<TestResponse>builder()
                .data(testService.createTestWithExcel(request, file))
                .build();
    }

    @GetMapping("/test-set/{testSetId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<TestResponse>> getTestsByTestSetId(@PathVariable Long testSetId) {
        return ApiResponse.<List<TestResponse>>builder()
                .data(testService.getTestsByTestSetId(testSetId))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> deleteTest(@PathVariable Long id) {
        testService.deleteTest(id);
        return ApiResponse.<Void>builder()
                .message("Test deleted successfully")
                .build();
    }
}
