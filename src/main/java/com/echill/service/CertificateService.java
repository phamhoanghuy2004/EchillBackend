package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CertificateRequest;
import com.echill.dto.response.CertificateResponse;
import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.CertificateMapper;
import com.echill.repository.CertificateRepository;
import com.echill.repository.UserRepository;
import com.echill.service.persistence.CertificatePersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateService {
    CertificateRepository certificateRepository;
    UserRepository userRepository;
    CertificateMapper certificateMapper;
    CloudinaryService cloudinaryService;
    CertificatePersistenceService certificatePersistenceService;

    public List<CertificateResponse> getMyCertificates() {
        Long userId = SecurityUtils.getCurrentUserId();
        return certificateRepository.findByUserId(userId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();
    }

    public CertificateResponse createCertificate(CertificateRequest request, MultipartFile evidence) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        String evidenceUrl = null;
        String imagePublicId = null;
        if (evidence != null && !evidence.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(evidence, CloudinaryFolder.CERTIFICATE_IMAGE);
            evidenceUrl = uploadResult.get("url");
            imagePublicId = uploadResult.get("publicId");
        }

        return certificateMapper.toCertificateResponse(
                certificatePersistenceService.saveCertificate(request, user, evidenceUrl, imagePublicId)
        );
    }

    public CertificateResponse updateCertificate(Long id, CertificateRequest request, MultipartFile evidence) {
        Long userId = SecurityUtils.getCurrentUserId();

        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.CERTIFICATE_NOT_FOUND));

        // Security check: only the owner can update
        if (!certificate.getUser().getId().equals(userId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }

        String evidenceUrl = null;
        String imagePublicId = null;
        if (evidence != null && !evidence.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(evidence, CloudinaryFolder.CERTIFICATE_IMAGE);
            evidenceUrl = uploadResult.get("url");
            imagePublicId = uploadResult.get("publicId");
        }

        return certificateMapper.toCertificateResponse(
                certificatePersistenceService.updateCertificate(certificate, request, evidenceUrl, imagePublicId)
        );
    }

    public void deleteCertificate(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.CERTIFICATE_NOT_FOUND));

        // Security check
        if (!certificate.getUser().getId().equals(userId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }

        certificatePersistenceService.deleteCertificate(certificate);
    }

    public List<com.echill.dto.response.TopStudentResponse> getTopToeicStudents() {
        org.springframework.data.domain.Pageable topTen = org.springframework.data.domain.PageRequest.of(0, 10);
        List<Certificate> topCertificates = certificateRepository.findTopCertificates(com.echill.entity.enums.CertType.TOEIC_LR, topTen);

        return topCertificates.stream().map(cert -> {
            User u = cert.getUser();
            return com.echill.dto.response.TopStudentResponse.builder()
                    .id(cert.getId())
                    .name(u.getFullName())
                    .avatar(u.getAvatarUrl() != null ? u.getAvatarUrl() : "https://i.pravatar.cc/150?u=" + u.getId())
                    .scores(com.echill.dto.response.TopStudentResponse.TopScoreDto.builder()
                            .listening(cert.getListeningScore())
                            .reading(cert.getReadingScore())
                            .total(cert.getTotalScore())
                            .build())
                    .build();
        }).toList();
    }
}
