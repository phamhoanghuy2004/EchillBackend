package com.echill.controller;

import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CloudinarySignatureResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.dto.response.PermissionResponse;
import com.echill.service.CloudinaryVideoService;
import com.echill.service.LessonService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonController {
    CloudinaryVideoService cloudinaryVideoService;
    LessonService lessonService;

    @GetMapping("/generateVideoUploadSignature")
    @PreAuthorize("hasAnyRole('TEACHER')")
    public ApiResponse<CloudinarySignatureResponse> getAll() {
        return ApiResponse.<CloudinarySignatureResponse>builder()
                .data(cloudinaryVideoService.generateVideoUploadSignature())
                .build();
    }

    @PutMapping("/{lessonId}/video-draft")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<LessonResponse> saveVideoDraft(
            @PathVariable Long lessonId,
            @Valid @RequestBody SaveVideoDraftRequest request) {

        return ApiResponse.<LessonResponse>builder()
                .message("Đã lưu bản nháp video. Hệ thống đang tiến hành xử lý!")
                .data(lessonService.saveVideoDraft(lessonId, request))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')") // Chỉ giáo viên mới có quyền tạo
    public ApiResponse<LessonResponse> createLesson(@Valid @RequestBody LessonCreationRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .message("Tạo nội dung bài học thành công!")
                .data(lessonService.createLesson(request))
                .build();
    }
}
