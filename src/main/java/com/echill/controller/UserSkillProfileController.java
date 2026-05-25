package com.echill.controller;

import com.echill.dto.request.SkillInsightRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.SkillInsightResponse;
import com.echill.dto.response.learner.AdaptiveLearningResponse;
import com.echill.entity.Lesson;
import com.echill.entity.UserSkillProfile;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.service.LessonService;
import com.echill.service.UserSkillProfileService;
import com.echill.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users/skills")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSkillProfileController {
    UserSkillProfileService profileService;
    UserSkillProfileRepository profileRepository;
    LessonService lessonService;

    @GetMapping("/insights")
    public ApiResponse<SkillInsightResponse> getSkillInsights(
            @Valid @ModelAttribute SkillInsightRequest request
    ) {
        SkillInsightResponse response = profileService.getSkillInsightsByGroup(request.getGroup());

        return ApiResponse.<SkillInsightResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{parentTagId}/children/insights")
    public ApiResponse<List<SkillInsightResponse.SkillDetail>> getChildSkillInsights(@PathVariable Long parentTagId) {
        return ApiResponse.<List<SkillInsightResponse.SkillDetail>>builder()
                .data(profileService.getChildSkillInsights(parentTagId))
                .build();
    }

    // ===== ADAPTIVE LEARNING =====

    @GetMapping("/adaptive-recommendation")
    public ApiResponse<AdaptiveLearningResponse> getAdaptiveRecommendation() {
        Long userId = SecurityUtils.getCurrentUserId();

        // Bước 0: Kiểm tra user đã có skill profile chưa (chưa làm placement test)
        if (!profileRepository.existsByUserId(userId)) {
            return ApiResponse.<AdaptiveLearningResponse>builder()
                    .data(AdaptiveLearningResponse.builder()
                            .status("NO_PROFILE").build())
                    .build();
        }

        // Bước 1-3: Tìm tag lỗ hổng ưu tiên nhất
        Optional<UserSkillProfile> topGap = profileService.findTopKnowledgeGap(userId);
        if (topGap.isEmpty()) {
            return ApiResponse.<AdaptiveLearningResponse>builder()
                    .data(AdaptiveLearningResponse.builder()
                            .status("NO_GAP").build())
                    .build();
        }

        UserSkillProfile gap = topGap.get();
        int targetLevel = profileService.getTargetLevel(userId);

        // Bước 4: Tìm bài học trong khóa đã mua
        Optional<Lesson> lesson = lessonService.findLessonForGapTag(userId, gap.getTag().getId());

        if (lesson.isPresent()) {
            Lesson l = lesson.get();
            return ApiResponse.<AdaptiveLearningResponse>builder()
                    .data(AdaptiveLearningResponse.builder()
                            .status("LESSON_FOUND")
                            .gapTagId(gap.getTag().getId())
                            .gapTagName(gap.getTag().getName())
                            .gapCurrentLevel(gap.getCurrentLevel())
                            .gapTargetLevel(targetLevel)
                            .lessonId(l.getId())
                            .lessonTitle(l.getTitle())
                            .courseId(l.getCourse().getId())
                            .courseName(l.getCourse().getName())
                            .courseImage(l.getCourse().getImageUrl())
                            .build())
                    .build();
        }

        // Bước 5: Upsell - không có bài học nào dạy tag này trong khóa đã mua
        return ApiResponse.<AdaptiveLearningResponse>builder()
                .data(AdaptiveLearningResponse.builder()
                        .status("UPSELL")
                        .gapTagId(gap.getTag().getId())
                        .gapTagName(gap.getTag().getName())
                        .gapCurrentLevel(gap.getCurrentLevel())
                        .gapTargetLevel(targetLevel)
                        .build())
                .build();
    }
}
