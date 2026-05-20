package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.admin.AdminFiltersResponse;
import com.echill.dto.response.admin.AdminSummaryResponse;
import com.echill.dto.response.admin.CourseRankingResponse;
import com.echill.dto.response.admin.TeacherRankingResponse;
import com.echill.dto.response.teacher.RevenueChartResponse;
import com.echill.service.AdminAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admins/analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> getSummary() {
        return ApiResponse.<AdminSummaryResponse>builder()
                .data(adminAnalyticsService.getSummary())
                .build();
    }

    @GetMapping("/revenue-chart")
    public ApiResponse<List<RevenueChartResponse>> getRevenueChart(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId) {
        return ApiResponse.<List<RevenueChartResponse>>builder()
                .data(adminAnalyticsService.getRevenueChart(fromDate, toDate, teacherId, courseId))
                .build();
    }

    @GetMapping("/teacher-rankings")
    public ApiResponse<List<TeacherRankingResponse>> getTeacherRankings() {
        return ApiResponse.<List<TeacherRankingResponse>>builder()
                .data(adminAnalyticsService.getTeacherRankings())
                .build();
    }

    @GetMapping("/course-rankings")
    public ApiResponse<List<CourseRankingResponse>> getCourseRankings() {
        return ApiResponse.<List<CourseRankingResponse>>builder()
                .data(adminAnalyticsService.getCourseRankings())
                .build();
    }

    @GetMapping("/filters")
    public ApiResponse<AdminFiltersResponse> getFilters() {
        return ApiResponse.<AdminFiltersResponse>builder()
                .data(adminAnalyticsService.getFilters())
                .build();
    }
}
