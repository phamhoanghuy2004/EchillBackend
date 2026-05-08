package com.echill.controller;

import com.echill.dto.request.MonthlyActivityRequest;
import com.echill.dto.request.StudyTimePingRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.MonthlyStudyActivityResponse;
import com.echill.dto.response.WeeklyStudyTimeResponse;
import com.echill.service.StudyAnalyticsService;
import com.echill.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudyAnalyticsController {
    StudyAnalyticsService studyAnalyticsService;

    /**
     * API: Ping thời gian học (Frontend gọi ngầm mỗi 30s)
     */
    @PostMapping("/ping")
    public ApiResponse<Void> pingStudyTime(@Valid @RequestBody StudyTimePingRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        studyAnalyticsService.pingStudyTime(userId, request.getAddedSeconds());

        return ApiResponse.<Void>builder()
                .message("Đồng bộ thời gian học thành công")
                .build();
    }

    /**
     * API: Lấy tổng thời gian học trong tuần hiện tại
     */
    @GetMapping("/weekly-seconds")
    public ApiResponse<WeeklyStudyTimeResponse> getWeeklyStudySeconds() {
        WeeklyStudyTimeResponse responseData = studyAnalyticsService.getTotalStudySecondsThisWeek();

        return ApiResponse.<WeeklyStudyTimeResponse>builder()
                .message("Lấy tổng số giây học trong tuần thành công")
                .data(responseData)
                .build();
    }

    @GetMapping("/monthly-activity")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<MonthlyStudyActivityResponse> getMonthlyActivity(
            @Valid @ModelAttribute MonthlyActivityRequest request) {

        return ApiResponse.<MonthlyStudyActivityResponse>builder()
                .message("Lấy dữ liệu học tập tháng thành công")
                .data(studyAnalyticsService.getMonthlyStudyActivity(request.getYear(), request.getMonth()))
                .build();
    }
}
