package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.teacher.RevenueChartResponse;
import com.echill.dto.response.teacher.TeacherSummaryResponse;
import com.echill.dto.response.teacher.TopCourseResponse;
import com.echill.service.TeacherAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teachers/analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherAnalyticsController {

    TeacherAnalyticsService teacherAnalyticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeacherSummaryResponse> getSummary(@RequestParam(required = false) Long courseId) {
        return ApiResponse.<TeacherSummaryResponse>builder()
                .data(teacherAnalyticsService.getSummary(courseId))
                .build();
    }

    @GetMapping("/revenue-chart")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<RevenueChartResponse>> getRevenueChart(
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "MONTH") String period) {
        return ApiResponse.<List<RevenueChartResponse>>builder()
                .data(teacherAnalyticsService.getRevenueChart(courseId, period))
                .build();
    }

    @GetMapping("/top-courses")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<TopCourseResponse>> getTopCourses() {
        return ApiResponse.<List<TopCourseResponse>>builder()
                .data(teacherAnalyticsService.getTopSellingCourses())
                .build();
    }

    @GetMapping("/my-courses-basic")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<Map<String, Object>>> getMyCoursesBasic() {
        return ApiResponse.<List<Map<String, Object>>>builder()
                .data(teacherAnalyticsService.getMyCoursesBasicInfo())
                .build();
    }
}
