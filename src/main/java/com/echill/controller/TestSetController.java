package com.echill.controller;

import com.echill.dto.request.TestSetRequest;
import com.echill.dto.request.TestSetUpdateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.TestSetDetailWithHistoryResponse;
import com.echill.dto.response.TestSetResponse;
import com.echill.dto.response.learner.TestSetRecommendationResponse;
import com.echill.service.TestSetService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-sets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class    TestSetController {
    TestSetService testSetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'ADMIN')")
    public ApiResponse<TestSetResponse> createTestSet(@RequestBody @Valid TestSetRequest request) {
        return ApiResponse.<TestSetResponse>builder()
                .data(testSetService.createTestSet(request))
                .build();
    }

    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<TestSetResponse> getTestSetByLessonId(@PathVariable Long lessonId) {
        return ApiResponse.<TestSetResponse>builder()
                .data(testSetService.getTestSetByLessonId(lessonId))
                .build();
    }

    @GetMapping("/{id}/history")
    public ApiResponse<TestSetDetailWithHistoryResponse> getTestSetDetailWithHistory(@PathVariable("id") Long testSetId) {
        return ApiResponse.<TestSetDetailWithHistoryResponse>builder()
                .data(testSetService.getTestSetDetailWithHistory(testSetId))
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<java.util.List<TestSetResponse>> getAllTestSets() {
        return ApiResponse.<java.util.List<TestSetResponse>>builder()
                .data(testSetService.getAllTestSets())
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ApiResponse<TestSetResponse> updateTestSet(
            @PathVariable Long id,
            @RequestBody @Valid TestSetUpdateRequest request) {
        return ApiResponse.<TestSetResponse>builder()
                .data(testSetService.updateTestSet(id, request))
                .build();
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<TestSetRecommendationResponse>> getRecommendations() {
        return ApiResponse.<List<TestSetRecommendationResponse>>builder()
                .message("Lấy danh sách đề xuất thành công")
                .data(testSetService.getNewestTestSetsForCurrentYear())
                .build();
    }
}
