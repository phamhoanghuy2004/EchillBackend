package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CertificateRequest;
import com.echill.dto.response.CertificateResponse;
import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.CertificateMapper;
import com.echill.repository.CertificateRepository;
import com.echill.repository.TeacherProfileRepository;
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
    TeacherProfileRepository teacherProfileRepository;
    CertificateMapper certificateMapper;
    CloudinaryService cloudinaryService;
    CertificatePersistenceService certificatePersistenceService;

    public List<CertificateResponse> getMyCertificates() {
        Long userId = SecurityUtils.getCurrentUserId();
        return certificateRepository.findByTeacherProfileId(userId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();
    }

    @Transactional
    public CertificateResponse createCertificate(CertificateRequest request, MultipartFile evidence) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        TeacherProfile profile = teacherProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.PROFILE_NOT_FOUND));

        String evidenceUrl = null;
        String imagePublicId = null;
        if (evidence != null && !evidence.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(evidence, CloudinaryFolder.CERTIFICATE_IMAGE);
            evidenceUrl = uploadResult.get("url");
            imagePublicId = uploadResult.get("publicId");
        } else {
            throw new AppException(ErrorEnum.INVALID_IMAGE_FORMAT); // Evidence is required for new certificate
        }

        return certificateMapper.toCertificateResponse(
                certificatePersistenceService.saveCertificate(request, profile, evidenceUrl, imagePublicId)
        );
    }

    @Transactional
    public CertificateResponse updateCertificate(Long id, CertificateRequest request, MultipartFile evidence) {
        Long userId = SecurityUtils.getCurrentUserId();

        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.CERTIFICATE_NOT_FOUND));

        // Security check: only the owner can update
        if (!certificate.getTeacherProfile().getId().equals(userId)) {
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

    @Transactional
    public void deleteCertificate(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.CERTIFICATE_NOT_FOUND));

        // Security check
        if (!certificate.getTeacherProfile().getId().equals(userId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }

        certificatePersistenceService.deleteCertificate(certificate);
    }
}
