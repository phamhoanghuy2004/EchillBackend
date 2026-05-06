package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.ImportTestResponse;
import com.echill.dto.response.TestResponse;
import com.echill.entity.Question;
import com.echill.entity.QuestionGroup;
import com.echill.entity.Test;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.TestMapper;
import com.echill.repository.QuestionGroupRepository;
import com.echill.repository.QuestionRepository;
import com.echill.repository.TestRepository;
import com.echill.service.AdminTestService;
import com.echill.service.CloudinaryService;
import com.echill.service.ImportTestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminTestController {

    ImportTestService importTestService;
    AdminTestService adminTestService;

    @PostMapping("/tests/import-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportTestResponse> importToeicTest(
            @RequestParam("testSetId") Long testSetId,
            @RequestParam("title") String title,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<ImportTestResponse>builder()
                .data(importTestService.importFromExcel(testSetId, title, file))
                .build();
    }

    @GetMapping("/tests/{testId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TestResponse> getTestForAdmin(@PathVariable Long testId) {
        return ApiResponse.<TestResponse>builder()
                .data(adminTestService.getTestById(testId))
                .build();
    }

    @PostMapping("/questions/{id}/audio")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> uploadQuestionAudio(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.<String>builder()
                .data(adminTestService.uploadQuestionAudio(id, file))
                .message("Upload audio successfully")
                .build();
    }

    @PostMapping("/questions/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> uploadQuestionImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.<String>builder()
                .data(adminTestService.uploadQuestionImage(id, file))
                .message("Upload image successfully")
                .build();
    }

    @PostMapping("/groups/{id}/audio")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> uploadGroupAudio(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.<String>builder()
                .data(adminTestService.uploadGroupAudio(id, file))
                .message("Upload audio successfully")
                .build();
    }

    @PostMapping("/groups/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> uploadGroupImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.<String>builder()
                .data(adminTestService.uploadGroupImage(id, file))
                .message("Upload image successfully")
                .build();
    }
}
