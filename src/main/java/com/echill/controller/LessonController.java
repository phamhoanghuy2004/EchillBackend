package com.echill.controller;

import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.request.leaner.SyncProgressRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CloudinarySignatureResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.dto.response.learner.CurriculumResponse;
import com.echill.dto.response.learner.LessonDetailResponse;
import com.echill.entity.enums.LessonStatus;
import com.echill.service.CloudinaryVideoService;
import com.echill.service.EnrollmentService;
import com.echill.service.LessonProgressService;
import com.echill.service.LessonService;
import com.echill.service.persistence.LessonPersistenceService;
import com.echill.service.redis.ProgressRedisService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonController {
    CloudinaryVideoService cloudinaryVideoService;
    LessonService lessonService;
    LessonPersistenceService lessonPersistenceService;
    EnrollmentService enrollmentService;
    ProgressRedisService progressRedisService;
    LessonProgressService lessonProgressService;

    @GetMapping("/generateVideoUploadSignature")
    @PreAuthorize("hasAnyRole('TEACHER')")
    public ApiResponse<CloudinarySignatureResponse> getSignature(@RequestParam Long lessonId) {
        return ApiResponse.<CloudinarySignatureResponse>builder()
                .data(cloudinaryVideoService.generateVideoUploadSignature(lessonId))
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

    @PutMapping("/{lessonId}/video-draft")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<LessonResponse> saveVideoDraft(@PathVariable Long lessonId, @Valid @RequestBody SaveVideoDraftRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .data(lessonPersistenceService.saveVideoDraft(lessonId, request))
                .message("Đã lưu bản nháp video, đang chờ xử lý...")
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

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> deleteLesson(@PathVariable Long lessonId) {
        lessonService.deleteLesson(lessonId);
        return ApiResponse.<Void>builder()
                .message("Xóa bài học thành công!")
                .build();
    }

    @PostMapping("/{lessonId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Map<String, Object>> startLesson(@PathVariable("lessonId") Long lessonId) {

        enrollmentService.startLesson(lessonId);

        Map<String, Object> response = new HashMap<>();
        response.put("lessonId", lessonId.toString());
        response.put("status", LessonStatus.IN_PROGRESS);

        return ApiResponse.<Map<String, Object>>builder()
                .message("Bắt đầu bài học thành công!")
                .data(response)
                .build();

    }

    @GetMapping("/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<LessonDetailResponse> getLessonDetailForStudy(@PathVariable("lessonId") Long lessonId) {
        return ApiResponse.<LessonDetailResponse>builder()
                .message("Lấy thông tin bài học thành công")
                .data(enrollmentService.getLessonDetailForStudy(lessonId))
                .build();
    }

    @PutMapping("/{lessonId}/progress")
    public ApiResponse<Void> syncVideoProgress(
            @PathVariable Long lessonId,
            @Valid @RequestBody SyncProgressRequest request) {

        progressRedisService.recordHeartbeat(lessonId, request.getCurrentSecond(), request.getPlaybackSpeed());
        return ApiResponse.<Void>builder()
                .message("Cập nhật tiến độ video thành công!")
                .build();
    }

    @GetMapping("/{lessonId}/progress")
    public ApiResponse<Integer> getCurrentProgress(@PathVariable Long lessonId) {

        Integer currentSecond = progressRedisService.getCurrentProgress(lessonId);

        return ApiResponse.<Integer>builder()
                .message("Lấy tiến độ video thành công!")
                .data(currentSecond)
                .build();
    }


    @PostMapping("/{lessonId}/complete")
    public ApiResponse<Void> completeVideoProgress(@PathVariable Long lessonId) {

        lessonProgressService.markVideoAsWatched(lessonId);

        return ApiResponse.<Void>builder()
                .message("Chúc mừng! Đã hoàn thành video bài học.")
                .build();
    }
}
