package com.echill.controller;

import com.echill.dto.NextStepResponse;
import com.echill.dto.request.SubmitAnswerRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.service.PlacementTestOrchestrator;
import com.echill.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/placement-test")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlacementTestController {

    PlacementTestOrchestrator orchestrator;

    /**
     * API: BẮT ĐẦU BÀI THI
     * Postman Test: POST /api/placement-test/start
     */
    @PostMapping("/start")
    public ApiResponse<NextStepResponse> startTest() {
        // 🟢 Lấy userId an toàn từ Security Context, dẹp bỏ RequestHeader
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("📥 API Nhận request bắt đầu bài thi từ User {}", userId);
        NextStepResponse response = orchestrator.startTest(userId);

        return ApiResponse.<NextStepResponse>builder()
                .code(1000)
                .message("Bắt đầu bài đánh giá năng lực thành công")
                .data(response)
                .build();
    }

    /**
     * API: NỘP ĐÁP ÁN VÀ LẤY CÂU TIẾP THEO
     * Postman Test: POST /api/placement-test/submit
     */
    @PostMapping("/submit")
    public ApiResponse<NextStepResponse> submitAnswer(@Valid @RequestBody SubmitAnswerRequest request) {
        // 🟢 Lấy userId an toàn từ Security Context
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("📥 API Nhận câu trả lời từ User {} cho câu hỏi {}", userId, request.getQuestionId());

        NextStepResponse response = orchestrator.submitAnswer(
                userId,
                request.getQuestionId(),
                request.getSelectedAnswerId()
        );

        return ApiResponse.<NextStepResponse>builder()
                .code(1000)
                .message(response.getMessage())
                .data(response)
                .build();
    }
}