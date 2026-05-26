package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.service.PersonalizedTestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quizzes/personalized")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalizedTestController {

    PersonalizedTestService personalizedTestService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TestPracticeResponse> generatePersonalizedTest() {
        return ApiResponse.<TestPracticeResponse>builder()
                .message("Sinh đề luyện tập cá nhân hóa thành công!")
                .data(personalizedTestService.generatePersonalizedTest())
                .build();
    }
}
