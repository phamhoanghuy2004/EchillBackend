package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.teacher.CourseDetailReportResponse;
import com.echill.dto.response.teacher.RevenueChartResponse;
import com.echill.dto.response.teacher.TeacherSummaryResponse;
import com.echill.dto.response.teacher.TopCourseResponse;
import com.echill.service.TeacherAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.<List<RevenueChartResponse>>builder()
                .data(teacherAnalyticsService.getRevenueChart(courseId, period, year))
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

    @GetMapping("/top-courses/detail")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<CourseDetailReportResponse>> getTopCoursesDetail(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "REVENUE") String sortBy) {
        Instant from = fromDate != null ? fromDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant to = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        return ApiResponse.<List<CourseDetailReportResponse>>builder()
                .data(teacherAnalyticsService.getTopCoursesDetailReport(from, to, sortBy))
                .build();
    }
}
