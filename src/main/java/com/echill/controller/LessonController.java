package com.echill.controller;

import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.request.leaner.SyncProgressRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CloudinarySignatureResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.dto.response.learner.CurriculumResponse;
import com.echill.dto.response.learner.LessonDetailResponse;
import com.echill.dto.response.learner.ProgressStatusResponse;
import com.echill.dto.response.learner.VideoCompleteResponse;
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
    public ApiResponse<ProgressStatusResponse> getCurrentProgress(@PathVariable Long lessonId) {

        ProgressStatusResponse response = progressRedisService.getCurrentProgress(lessonId);

        return ApiResponse.<ProgressStatusResponse>builder()
                .message("Lấy tiến độ video thành công!")
                .data(response)
                .build();
    }


    @PostMapping("/{lessonId}/complete")
    public ApiResponse<VideoCompleteResponse> completeVideoProgress(@PathVariable Long lessonId) {
        return ApiResponse.<VideoCompleteResponse>builder()
                .message("Chúc mừng! Đã hoàn thành video bài học.")
                .data(lessonProgressService.markVideoAsWatched(lessonId))
                .build();
    }

    @GetMapping("/weekly-completed-lessons")
    public ApiResponse<Long> getWeeklyCompletedLessons() {
        Long completedCount = lessonProgressService.getCompletedLessonsCountThisWeek();

        return ApiResponse.<Long>builder()
                .message("Lấy số lượng bài học hoàn thành trong tuần thành công")
                .data(completedCount)
                .build();
    }

    @GetMapping("/weekly-completed-tests")
    public ApiResponse<Long> getWeeklyCompletedTests() {
        Long completedCount = lessonProgressService.getCompletedTestsCountThisWeek();

        return ApiResponse.<Long>builder()
                .message("Lấy số lượng bài test đã hoàn thành trong tuần thành công")
                .data(completedCount)
                .build();
    }

    @PostMapping("/{lessonId}/chat")
    public ApiResponse<com.echill.dto.response.DocumentChatResponse> chatWithLesson(
            @PathVariable Long lessonId,
            @jakarta.validation.Valid @RequestBody com.echill.dto.request.DocumentChatRequest request) {
        return ApiResponse.<com.echill.dto.response.DocumentChatResponse>builder()
                .message("Phản hồi từ AI")
                .data(lessonService.chatWithLesson(lessonId, request.getQuestion()))
                .build();
    }
}
