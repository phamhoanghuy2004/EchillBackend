package com.echill.controller;

import com.echill.dto.request.AdminCourseSearchRequest;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.PageResponse;
import com.echill.entity.enums.Status;
import com.echill.service.AdminCourseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admins/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCourseController {

    AdminCourseService adminCourseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<CourseCardResponse>> getCourses(
            @Valid @ModelAttribute AdminCourseSearchRequest request) {
        return ApiResponse.<PageResponse<CourseCardResponse>>builder()
                .data(adminCourseService.getCourses(request))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponse> getCourseDetail(@PathVariable Long id) {
        return ApiResponse.<CourseResponse>builder()
                .data(adminCourseService.getCourseDetail(id))
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponse> updateCourseStatus(
            @PathVariable Long id,
            @RequestParam Status status) {
        return ApiResponse.<CourseResponse>builder()
                .data(adminCourseService.updateCourseStatus(id, status))
                .build();
    }
}
