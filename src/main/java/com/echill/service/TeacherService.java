package com.echill.service;

import com.echill.dto.request.TeacherProfileUpdateRequest;
import com.echill.dto.response.CertificateResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.TeacherResponse;
import com.echill.dto.response.TeacherStudentResponse;
import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.CertificateRepository;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.TeacherProfileRepository;
import com.echill.repository.UserRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherService {

    UserRepository userRepository;
    TeacherProfileRepository teacherProfileRepository;
    CertificateRepository certificateRepository;
    EnrollmentRepository enrollmentRepository;

    // 💥 Không cần @Transactional, Spring Data JPA tự lo vụ readOnly rồi
    public TeacherResponse getMyProfile() {
        // 1. Lấy thẳng ID từ Security Context
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. Kéo Data trực tiếp từ Repository & Validate tại chỗ
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        TeacherProfile profile = teacherProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.PROFILE_NOT_FOUND));

        // Profile chắc chắn tồn tại (nếu không đã throw ở trên), lấy luôn list chứng chỉ
        List<Certificate> certificates = certificateRepository.findByTeacherProfileId(profile.getId());

        // 3. Lấy danh sách Roles
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());

        // 4. Map dữ liệu trả về
        return buildTeacherResponse(user, profile, certificates, roles);
    }

    @Transactional
    public void updateProfile(TeacherProfileUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        TeacherProfile profile = teacherProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.PROFILE_NOT_FOUND));

        profile.setBio(request.getBio());
        
        teacherProfileRepository.save(profile);
    }

    // ✅ Trả về List để Frontend tự xử lý bộ lọc theo yêu cầu
    public List<TeacherStudentResponse> getStudentStatistics() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return enrollmentRepository.findStudentStatistics(teacherId);
    }

    private TeacherResponse buildTeacherResponse(User user, TeacherProfile profile, List<Certificate> certificates, Set<String> roles) {

        List<CertificateResponse> certResponses = certificates.stream()
                .map(cert -> CertificateResponse.builder()
                        .id(cert.getId())
                        .certType(cert.getCertType())
                        .totalScore(cert.getTotalScore())
                        .listeningScore(cert.getListeningScore())
                        .readingScore(cert.getReadingScore())
                        .speakingScore(cert.getSpeakingScore())
                        .writingScore(cert.getWritingScore())
                        .issuedDate(cert.getIssuedDate())
                        .evidenceUrl(cert.getEvidenceUrl())
                        .build())
                .toList();

        return TeacherResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .dob(user.getDob())
                .jobTitle(user.getJobTitle())
                .avatarUrl(user.getAvatarUrl())
                .currentCoin(user.getCurrentCoin())
                .bio(profile.getBio())
                .certificates(certResponses)
                .roles(roles)
                .build();
    }
}