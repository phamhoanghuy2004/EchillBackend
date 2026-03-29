package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.TeacherResponse;
import com.echill.service.TeacherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
