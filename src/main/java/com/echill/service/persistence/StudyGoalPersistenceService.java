package com.echill.service.persistence;

import com.echill.dto.request.StudyGoalRequest;
import com.echill.entity.StudentProfile;
import com.echill.entity.StudyGoal;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.mapper.StudyGoalMapper;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.StudyGoalRepository;
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
public class StudyGoalPersistenceService {

    StudyGoalRepository studyGoalRepository;
    StudentProfileRepository studentProfileRepository;
    StudyGoalMapper studyGoalMapper;

    @Transactional
    public StudyGoal createNewGoal(Long userId, StudyGoalRequest request) {
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

        return studyGoalRepository.save(newGoal);
    }

    @Transactional
    public StudyGoal updateGoal(Long goalId, Long userId, StudyGoalRequest request) {
        StudyGoal existingGoal = studyGoalRepository.findByIdAndStudentProfileIdAndIsActiveTrue(goalId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.GOAL_NOT_FOUND));

        studyGoalMapper.updateStudyGoal(existingGoal, request);
        log.info("Đã cập nhật mục tiêu ID {} của user {}", goalId, userId);

        return existingGoal; // Dirty checking tự động lưu
    }
}