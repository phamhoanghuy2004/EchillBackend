package com.echill.service;

import com.echill.dto.response.PlacementTestStatusResponse;
import com.echill.dto.response.StudentResponse;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.entity.StudentProfile;
import com.echill.entity.StudyGoal;
import com.echill.entity.User;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.Level;
import com.echill.entity.enums.TagGroup;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.StudyGoalRepository;
import com.echill.repository.UserRepository;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StudentService {

    UserRepository userRepository;
    StudentProfileRepository studentProfileRepository;
    StudyGoalRepository studyGoalRepository;
    UserSkillProfileRepository userSkillProfileRepository;

    public StudentResponse getMyProfile() {
        // 1. Lấy thẳng ID từ JWT (Nhanh, không chạm DB)
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. Lấy dữ liệu trực tiếp từ Repository & Validate tại chỗ
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        StudentProfile profile = studentProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.PROFILE_NOT_FOUND));

        StudyGoal activeGoal = null;
        if (profile.getId() != null) {
            activeGoal = studyGoalRepository.findByStudentProfileIdAndIsActiveTrue(profile.getId())
                    .orElse(null);
        }

        // 3. Xử lý Roles
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());

        // 4. Trả về kết quả
        return buildStudentResponse(user, profile, activeGoal, roles);
    }

    private StudentResponse buildStudentResponse(User user, StudentProfile profile, StudyGoal goal, Set<String> roles) {
        StudyGoalResponse goalResponse = null;

        if (goal != null) {
            goalResponse = StudyGoalResponse.builder()
                    .id(goal.getId())
                    .certType(goal.getCertType())
                    .targetListening(goal.getTargetListening())
                    .targetReading(goal.getTargetReading())
                    .targetSpeaking(goal.getTargetSpeaking())
                    .targetWriting(goal.getTargetWriting())
                    .targetTotal(goal.getTargetTotal())
                    .currentListening(goal.getCurrentListening())
                    .currentReading(goal.getCurrentReading())
                    .currentSpeaking(goal.getCurrentSpeaking())
                    .currentWriting(goal.getCurrentWriting())
                    .currentTotal(goal.getCurrentTotal())
                    .build();
        }

        return StudentResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .dob(user.getDob())
                .jobTitle(user.getJobTitle())
                .avatarUrl(user.getAvatarUrl())
                .currentCoin(user.getCurrentCoin())
                .level(profile.getLevel())
                .activeGoal(goalResponse)
                .roles(roles)
                .build();
    }

    @Transactional
    public void updateOverallStudentLevel(Long userId, boolean isPlacementTestFinished) {
        log.info("Bắt đầu tính toán Level Tổng cho User {}", userId);

        List<UserSkillProfile> childProfiles = userSkillProfileRepository.findChildProfilesByUserIdAndTagGroup(userId, TagGroup.ENGLISH_TOEIC);

        if (childProfiles.isEmpty()) return;

        double totalRatio = 0.0;
        int validProfilesCount = 0;

        for (UserSkillProfile p : childProfiles) {
            if (p.getTag() != null && p.getTag().getMaxLevel() != null && p.getTag().getMaxLevel() > 0) {
                int cappedLevel = Math.min(p.getCurrentLevel(), p.getTag().getMaxLevel());
                double ratio = (double) cappedLevel / p.getTag().getMaxLevel();
                totalRatio += ratio;
                validProfilesCount++;
            }
        }

        double avgRatio = validProfilesCount > 0 ? totalRatio / validProfilesCount : 0.0;

        Level newOverallLevel;
        if (avgRatio <= (1.0 / 3.0)) {
            newOverallLevel = Level.BEGINNER;
        } else if (avgRatio <= (2.0 / 3.0)) {
            newOverallLevel = Level.INTERMEDIATE;
        } else {
            newOverallLevel = Level.ADVANCED;
        }

        StudentProfile profile = studentProfileRepository.findById(userId)
                .orElse(StudentProfile.builder()
                        .user(userRepository.getReferenceById(userId))
                        .build());

        boolean isUpdated = false;

        if (profile.getLevel() != newOverallLevel) {
            profile.setLevel(newOverallLevel);
            isUpdated = true;
            log.info("🎉 User {} đã thăng cấp thành {} do lấp đầy toàn bộ móng!", userId, newOverallLevel);
        }

        if (isPlacementTestFinished && !profile.isPlacementTestCompleted()) {
            profile.setPlacementTestCompleted(true);
            isUpdated = true;
            log.info("✅ Đã bật cờ hoàn thành Placement Test cho User {}", userId);
        }

        if (isUpdated) {
            studentProfileRepository.save(profile);
        }
    }

    @Transactional(readOnly = true)
    public PlacementTestStatusResponse checkPlacementTestStatus() {
        Long userId = SecurityUtils.getCurrentUserId();

        return studentProfileRepository.findByUserId(userId)
                .map(profile -> PlacementTestStatusResponse.builder()
                        .hasCompleted(profile.isPlacementTestCompleted())
                        .currentLevel(profile.getLevel().name())
                        .build())
                .orElse(PlacementTestStatusResponse.builder()
                        .hasCompleted(false)
                        .currentLevel("UNDETERMINED")
                        .build());
    }
}