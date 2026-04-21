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

    // 💥 Gắn thẳng Transactional ở đây vì không có API ngoài (Network I/O)
    @Transactional
    public StudyGoalResponse createNewGoal(StudyGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 💥 TUYỆT CHIÊU PROXY: Tạo reference ảo, không tốn 1 dòng SQL SELECT nào!
        StudentProfile profileRef = studentProfileRepository.getReferenceById(userId);

        // Hủy mục tiêu cũ nếu có
        studyGoalRepository.findByStudentProfileIdAndIsActiveTrue(userId)
                .ifPresent(activeGoal -> {
                    activeGoal.markAsInactive(); // Hành vi DDD chuẩn!
                    log.info("Đã vô hiệu hóa mục tiêu cũ của userId: {}", userId);
                });

        StudyGoal newGoal = studyGoalMapper.toStudyGoal(request);
        newGoal.setStudentProfile(profileRef); // Set bằng Proxy

        newGoal = studyGoalRepository.save(newGoal);

        return studyGoalMapper.toStudyGoalResponse(newGoal);
    }

    // 💥 Gắn thẳng Transactional, dùng Dirty Checking
    @Transactional
    public StudyGoalResponse updateGoal(Long goalId, StudyGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        StudyGoal existingGoal = studyGoalRepository.findByIdAndStudentProfileIdAndIsActiveTrue(goalId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.GOAL_NOT_FOUND));

        studyGoalMapper.updateStudyGoal(existingGoal, request);
        log.info("Đã cập nhật mục tiêu ID {} của user {}", goalId, userId);

        // Dirty checking tự động lưu xuống DB, chỉ cần map ra trả về
        return studyGoalMapper.toStudyGoalResponse(existingGoal);
    }

    @Transactional(readOnly = true)
    public StudyGoalResponse getMyActiveGoal() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.debug("[StudyGoal] Đang lấy mục tiêu active của userId: {}", userId);

        // Sử dụng luôn hàm findByStudentProfileIdAndIsActiveTrue đã có sẵn
        StudyGoal activeGoal = studyGoalRepository.findByStudentProfileIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.GOAL_NOT_FOUND));

        return studyGoalMapper.toStudyGoalResponse(activeGoal);
    }
}