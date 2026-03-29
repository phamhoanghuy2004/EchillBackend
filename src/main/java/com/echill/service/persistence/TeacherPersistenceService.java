package com.echill.service.persistence;

import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.CertificateRepository;
import com.echill.repository.TeacherProfileRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherPersistenceService {

    UserRepository userRepository;
    TeacherProfileRepository teacherProfileRepository;
    CertificateRepository certificateRepository;

    @Transactional(readOnly = true)
    public User getUserWithRolesById(Long userId) {
        return userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
    }

    @Transactional(readOnly = true)
    public TeacherProfile getProfileByUserId(Long userId) {
        return teacherProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.PROFILE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Certificate> getCertificatesByProfileId(Long profileId) {
        if (profileId == null) return Collections.emptyList();
        return certificateRepository.findByTeacherProfileId(profileId);
    }
}