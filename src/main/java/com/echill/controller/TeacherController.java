package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.StudentResponse;
import com.echill.dto.response.TeacherResponse;
import com.echill.service.StudentService;
import com.echill.service.TeacherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
    public ApiResponse<TeacherResponse> getMyProfile() {
        return ApiResponse.<TeacherResponse>builder()
                .data(teacherService.getMyProfile())
                .build();
    }
}
