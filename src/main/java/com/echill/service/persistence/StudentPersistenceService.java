package com.echill.service.persistence;

import com.echill.entity.StudentProfile;
import com.echill.entity.StudyGoal;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.StudyGoalRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentPersistenceService {

    UserRepository userRepository;
    StudentProfileRepository studentProfileRepository;
    StudyGoalRepository studyGoalRepository;

    @Transactional(readOnly = true)
    public User getUserWithRolesById(Long userId) {
        return userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
    }

    @Transactional(readOnly = true)
    public StudentProfile getProfileByUserId(Long userId) {
        return studentProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.PROFILE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public StudyGoal getActiveGoalByProfileId(Long profileId) {
        if (profileId == null) return null;
        return studyGoalRepository.findByStudentProfileIdAndIsActiveTrue(profileId).orElse(null);
    }
}