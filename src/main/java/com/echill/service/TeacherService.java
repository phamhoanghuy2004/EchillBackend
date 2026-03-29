package com.echill.service;

import com.echill.dto.response.CertificateResponse;
import com.echill.dto.response.TeacherResponse;
import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.entity.User;
import com.echill.service.persistence.TeacherPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherService {

    TeacherPersistenceService teacherPersistenceService;

    // 💥 KHÔNG CẦN @Transactional Ở ĐÂY
    public TeacherResponse getMyProfile() {
        // 1. Lấy thẳng ID từ Security Context (Nhanh, không chạm DB)
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. Kéo Data từ Persistence Layer
        User user = teacherPersistenceService.getUserWithRolesById(userId);
        TeacherProfile profile = teacherPersistenceService.getProfileByUserId(userId);

        // 💥 DỌN DẸP: Không cần check null nữa, cứ thế mà lấy luôn vì profile đã chắc kèo tồn tại!
        List<Certificate> certificates = teacherPersistenceService.getCertificatesByProfileId(profile.getId());

        // 3. Lấy danh sách Roles
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());

        // 4. Map dữ liệu trả về
        return buildTeacherResponse(user, profile, certificates, roles);
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