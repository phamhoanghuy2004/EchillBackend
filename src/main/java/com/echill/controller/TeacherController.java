package com.echill.controller;

import com.echill.dto.request.TeacherProfileUpdateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.TeacherResponse;
import com.echill.dto.response.TeacherStudentResponse;
import com.echill.service.TeacherService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherController {
    TeacherService teacherService;

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeacherResponse> getMyProfile() {
        return ApiResponse.<TeacherResponse>builder()
                .data(teacherService.getMyProfile())
                .build();
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody TeacherProfileUpdateRequest request) {
        teacherService.updateProfile(request);
        return ApiResponse.<Void>builder()
                .message("Teacher profile updated successfully")
                .build();
    }

    @GetMapping("/student-statistics")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResponse<TeacherStudentResponse>> getStudentStatistics(
             @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<TeacherStudentResponse>>builder()
                .data(teacherService.getStudentStatistics(pageable))
                .build();
    }
}
