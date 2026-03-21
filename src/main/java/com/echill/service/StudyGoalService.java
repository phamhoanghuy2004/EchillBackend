package com.echill.service;

import com.echill.dto.request.StudyGoalRequest;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.entity.StudentProfile;
import com.echill.entity.StudyGoal;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.mapper.StudyGoalMapper;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.StudyGoalRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StudyGoalService {
    StudyGoalRepository studyGoalRepository;
    StudentProfileRepository studentProfileRepository;
    StudyGoalMapper studyGoalMapper;

    @Transactional
    public StudyGoalResponse createNewGoal(StudyGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        StudentProfile profile = studentProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.PROFILE_NOT_FOUND));

        studyGoalRepository.findByStudentProfileIdAndIsActiveTrue(profile.getId())
                .ifPresent(activeGoal -> {
                    activeGoal.markAsInactive();
                    log.info("Đã vô hiệu hóa mục tiêu cũ của userId: {}", userId);
                });

        // Tạo mục tiêu mới
        StudyGoal newGoal = studyGoalMapper.toStudyGoal(request);
        newGoal.setStudentProfile(profile);

        newGoal = studyGoalRepository.save(newGoal);

        return studyGoalMapper.toStudyGoalResponse(newGoal);
    }

    @Transactional
    public StudyGoalResponse updateGoal(Long goalId, StudyGoalRequest request) {

        // 1. Lấy ID của user đang đăng nhập
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. Tìm mục tiêu cần sửa (Phải thỏa 3 điều kiện: Đúng ID + Đúng chủ sở hữu + Đang Active)
        StudyGoal existingGoal = studyGoalRepository.findByIdAndStudentProfileIdAndIsActiveTrue(goalId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.GOAL_NOT_FOUND));

        // 💥 3. DÙNG MAPSTRUCT ĐỂ CẬP NHẬT (Nó sẽ tự động phớt lờ các trường current, id, isActive)
        studyGoalMapper.updateStudyGoal(existingGoal, request);

        // (Không cần gọi .save() vì @Transactional sẽ kích hoạt Dirty Checking và tự UPDATE SQL)
        log.info("Đã cập nhật điểm Target cho mục tiêu ID {} của user {}", goalId, userId);

        // 4. Trả về dữ liệu mới
        return studyGoalMapper.toStudyGoalResponse(existingGoal);
    }


}
