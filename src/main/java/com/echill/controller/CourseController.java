package com.echill.controller;

import com.echill.dto.request.CourseRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CourseResponse;
import com.echill.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourseResponse> createCourse(
            @Valid @ModelAttribute CourseRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return ApiResponse.<CourseResponse>builder()
                .data(courseService.createCourse(request, file))
                .build();
    }

    @GetMapping("/teacher")
    public ApiResponse<List<CourseResponse>> getCoursesByTeacher() {
        return ApiResponse.<List<CourseResponse>>builder()
                .data(courseService.getAllCoursesByTeacher())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable Long id) {
        return ApiResponse.<CourseResponse>builder()
                .data(courseService.getCourseById(id))
                .build();
    }
}
