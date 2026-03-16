package com.echill.service;

import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.response.StudentResponse;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.entity.StudentProfile;
import com.echill.entity.StudyGoal;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.StudyGoalRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentService {
    StudentProfileRepository studentProfileRepository;
    StudyGoalRepository studyGoalRepository;
    UserRepository userRepository;

    @Transactional
    public void completeProfile(CompleteProfileRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorEnum.USER_NOTFOUND)
        );

        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        userRepository.save(user);
    }

    @Transactional(readOnly = true) // readOnly = true giúp tối ưu hiệu năng khi chỉ đọc dữ liệu
    public StudentResponse getMyProfile() {
        // 1. Lấy username của người đang gọi API từ JWT Token
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Lấy User kèm Roles
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 3. Lấy Profile (Có thể null với tài khoản ADMIN, TEACHER)
        StudentProfile profile = studentProfileRepository.findByUserUsernameWithUser(username)
                .orElse(null);

        // 4. Lấy StudyGoal đang Active nếu có profile
        StudyGoal activeGoal = null;
        if (profile != null) {
            activeGoal = studyGoalRepository.findByStudentProfileIdAndIsActiveTrue(profile.getId())
                    .orElse(null);
        }

        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());

        // 5. Map dữ liệu sang DTO và trả về
        return buildStudentResponse(user, profile, activeGoal, roles);
    }

    // Hàm phụ trợ để Map dữ liệu (Có thể dùng MapStruct nếu muốn, nhưng map tay kiểu này dễ debug hơn)
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
                .level(profile != null ? profile.getLevel() : null)
                .activeGoal(goalResponse) // Nếu chưa set mục tiêu, chỗ này sẽ là null
                .roles(roles)
                .build();
    }
}
