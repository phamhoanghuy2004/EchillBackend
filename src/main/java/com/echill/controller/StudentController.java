package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.StudentResponse;
import com.echill.service.StudentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentController {
    StudentService studentService;
    @GetMapping("/my-profile")
    public ApiResponse<StudentResponse> getMyProfile() {
        return ApiResponse.<StudentResponse>builder()
                .data(studentService.getMyProfile())
                .build();
    }
}
