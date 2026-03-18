package com.echill.controller;

import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.StudentResponse;
import com.echill.service.StudentService;
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
public class StudentController {
    StudentService studentService;
    @GetMapping("/my-profile")
    public ApiResponse<StudentResponse> getMyProfile() {
        return ApiResponse.<StudentResponse>builder()
                .data(studentService.getMyProfile())
                .build();
    }
}
