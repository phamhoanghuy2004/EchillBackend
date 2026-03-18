package com.echill.service;

import com.echill.dto.response.CertificateResponse;
import com.echill.dto.response.TeacherResponse;
import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.CertificateRepository;
import com.echill.repository.TeacherProfileRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherService {

    TeacherProfileRepository teacherProfileRepository;
    CertificateRepository certificateRepository;
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public TeacherResponse getMyProfile() {
        // 1. Lấy username từ Security Context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Kéo User (kèm role) từ DB
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 3. Kéo TeacherProfile
        TeacherProfile profile = teacherProfileRepository.findById(user.getId())
                .orElse(null);

        // 4. Kéo danh sách Chứng chỉ (Certificates) nếu profile tồn tại
        List<Certificate> certificates = Collections.emptyList();
        if (profile != null) {
            certificates = certificateRepository.findByTeacherProfileId(profile.getId());
        }

        // 5. Lấy danh sách Roles
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());

        // 6. Map dữ liệu trả về
        return buildTeacherResponse(user, profile, certificates, roles);
    }

    private TeacherResponse buildTeacherResponse(User user, TeacherProfile profile, List<Certificate> certificates, Set<String> roles) {

        // Map List<Certificate> sang List<CertificateResponse>
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
                .collect(Collectors.toList());

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
                .bio(profile != null ? profile.getBio() : null)
                .certificates(certResponses) // Nạp list chứng chỉ vào đây
                .roles(roles)
                .build();
    }

}
