package com.echill.service;

import com.echill.dto.response.StudentResponse;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.entity.StudentProfile;
import com.echill.entity.StudyGoal;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.StudyGoalRepository;
import com.echill.repository.UserRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentService {

    UserRepository userRepository;
    StudentProfileRepository studentProfileRepository;
    StudyGoalRepository studyGoalRepository;

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
}