package com.echill.controller;

import com.echill.dto.request.TestSetRequest;
import com.echill.dto.request.TestSetSearchRequest;
import com.echill.dto.request.TestSetUpdateRequest;
import com.echill.dto.response.*;
import com.echill.dto.response.learner.TestSetRecommendationResponse;
import com.echill.entity.enums.TestType;
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

    @GetMapping
    public ApiResponse<PageResponse<TestSetResponse>> getAllTestSets(
            @Valid @ModelAttribute TestSetSearchRequest request) {

        return ApiResponse.<PageResponse<TestSetResponse>>builder()
                .message("Lấy danh sách bộ đề thành công")
                .data(testSetService.searchTestSets(request))
                .build();
    }

    @GetMapping("/types")
    public ApiResponse<List<String>> getAllowedTestTypes() {

        // 🔴 Chỉ cần gọi hàm từ Enum, Controller không cần biết bên trong loại bỏ cái gì
        return ApiResponse.<List<String>>builder()
                .message("Lấy danh sách phân loại bộ đề thành công")
                .data(TestType.getPracticePageTypes())
                .build();
    }

    @GetMapping("/{testSetId}")
    public ApiResponse<TestSetDetailResponse> getTestSetDetail(
            @PathVariable Long testSetId) {

        TestSetDetailResponse responseData = testSetService.getTestSetDetail(testSetId);

        return ApiResponse.<TestSetDetailResponse>builder()
                .data(responseData)
                .message("Lấy chi tiết bộ đề thành công!")
                .build();
    }
}
