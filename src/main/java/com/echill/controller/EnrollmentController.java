package com.echill.controller;

import com.echill.dto.request.elasticsearch.request.CourseSearchRequest;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.dto.request.leaner.GetMyCoursesRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.learner.CurriculumResponse;
import com.echill.dto.response.learner.MyCourseResponse;
import com.echill.service.EnrollmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentController {

    EnrollmentService enrollmentService;

    @GetMapping("/my-course")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResponse<MyCourseResponse>> searchCourses(
            @ModelAttribute GetMyCoursesRequest request) { // Dùng @ModelAttribute để nhận query params từ URL

        PageResponse<MyCourseResponse> springPage = enrollmentService.getMyLearningCourses(request);

        return ApiResponse.<PageResponse<MyCourseResponse>>builder()
                .message("Lấy danh sách khóa học thành công!")
                .data(springPage)
                .build();
    }

    @GetMapping("/{courseId}/curriculum")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CurriculumResponse> getCourseCurriculum(
            @PathVariable("courseId") Long courseId) {
        return ApiResponse.<CurriculumResponse>builder()
                .data(enrollmentService.getCourseCurriculum(courseId))
                .build();
    }
}
