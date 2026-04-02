package com.echill.controller;

import com.echill.document.CourseDocument;
import com.echill.dto.request.CourseRequest;
import com.echill.dto.request.elasticsearch.request.CourseSearchRequest;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.PageResponse;
import com.echill.service.CourseSearchService;
import com.echill.service.CourseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {

    CourseService courseService;
    CourseSearchService courseSearchService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CourseResponse> createCourse(
            @Valid @ModelAttribute CourseRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return ApiResponse.<CourseResponse>builder()
                .data(courseService.createCourse(request, file))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<CourseCardResponse>> searchCourses(
            @ModelAttribute CourseSearchRequest request) { // Dùng @ModelAttribute để nhận query params từ URL

        Page<CourseCardResponse> springPage = courseSearchService.searchCourses(request);

        return ApiResponse.<PageResponse<CourseCardResponse>>builder()
                .message("Lấy danh sách khóa học thành công!")
                .data(PageResponse.of(springPage))
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @ModelAttribute CourseRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return ApiResponse.<CourseResponse>builder()
                .data(courseService.updateCourse(id, request, file))
                .build();
    }
}
