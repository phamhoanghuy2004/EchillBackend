package com.echill.controller;

import com.echill.dto.request.ReviewRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.ReviewResponse;
import com.echill.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewResponse> createOrUpdateReview(@RequestBody @Valid ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .data(reviewService.createOrUpdateReview(request))
                .build();
    }

    @GetMapping("/my-review/{courseId}")
    public ApiResponse<ReviewResponse> getMyReview(@PathVariable Long courseId) {
        return ApiResponse.<ReviewResponse>builder()
                .data(reviewService.getMyReviewByCourse(courseId))
                .build();
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<List<ReviewResponse>> getCourseReviews(@PathVariable Long courseId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .data(reviewService.getReviewsByCourse(courseId))
                .build();
    }

    @GetMapping("/course/{courseId}/paginated")
    public ApiResponse<PageResponse<ReviewResponse>> getPaginatedCourseReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<ReviewResponse> reviewPage = reviewService.getPaginatedReviewsByCourse(courseId, page, size);
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .data(PageResponse.of(reviewPage))
                .build();
    }
}
