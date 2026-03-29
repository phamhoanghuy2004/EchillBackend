package com.echill.controller;

import com.echill.dto.request.StudyGoalRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.StudentResponse;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.service.StudentService;
import com.echill.service.StudyGoalService;
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
    StudyGoalService studyGoalService;

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentResponse> getMyProfile() {
        return ApiResponse.<StudentResponse>builder()
                .data(studentService.getMyProfile())
                .build();
    }

    @PostMapping("/study-goals")
    @PreAuthorize("hasRole('STUDENT')") // Chỉ Student mới được gọi
    public ApiResponse<StudyGoalResponse> createStudyGoal(@Valid @RequestBody StudyGoalRequest request) {
        return ApiResponse.<StudyGoalResponse>builder()
                .message("Tạo mục tiêu học tập thành công!")
                .data(studyGoalService.createNewGoal(request))
                .build();
    }

    @PutMapping("/study-goals/{id}")
    @PreAuthorize("hasRole('STUDENT')") // Chỉ Student mới được gọi
    public ApiResponse<StudyGoalResponse> updateStudyGoal( @PathVariable("id") Long id,
                                                           @Valid @RequestBody StudyGoalRequest request) {
        return ApiResponse.<StudyGoalResponse>builder()
                .message("Cập nhật mục tiêu học tập thành công!")
                .data(studyGoalService.updateGoal(id,request))
                .build();
    }

}
