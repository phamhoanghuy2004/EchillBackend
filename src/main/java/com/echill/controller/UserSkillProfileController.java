package com.echill.controller;

import com.echill.dto.request.SkillInsightRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.SkillInsightResponse;
import com.echill.service.UserSkillProfileService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/skills")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSkillProfileController {
    UserSkillProfileService profileService;

    @GetMapping("/insights")
    public ApiResponse<SkillInsightResponse> getSkillInsights(
            @Valid @ModelAttribute SkillInsightRequest request
    ) {
        SkillInsightResponse response = profileService.getSkillInsightsByGroup(request.getGroup());

        return ApiResponse.<SkillInsightResponse>builder()
                .data(response)
                .build();
    }
}
