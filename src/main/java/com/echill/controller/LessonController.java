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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonCreationRequest request) {

        return ApiResponse.<LessonResponse>builder()
                .message("Cập nhật bài học thành công!")
                .data(lessonService.updateLesson(lessonId, request))
                .build();
    }

    @PostMapping(value = "/{lessonId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<LessonResponse> uploadDocument(
            @PathVariable Long lessonId,
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {

        return ApiResponse.<LessonResponse>builder()
                .message("Tải lên tài liệu thành công!")
                .data(lessonService.uploadDocument(lessonId, title, file))
                .build();
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        lessonService.deleteDocument(documentId);
        return ApiResponse.<Void>builder()
                .message("Đã xóa tài liệu thành công!")
                .build();
    }
}
