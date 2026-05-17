package com.echill.service.persistence;

import com.echill.dto.request.CertificateRequest;
import com.echill.entity.Certificate;
import com.echill.entity.TeacherProfile;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.CertificateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificatePersistenceService {
    CertificateRepository certificateRepository;

    @Transactional
    public Certificate saveCertificate(CertificateRequest request, User user, String evidenceUrl, String imagePublicId) {
        if(imagePublicId == null && evidenceUrl == null) {
            throw new AppException(TeacherErrorEnum.CERTIFICATE_REQUIRED);
        }
        Certificate certificate = Certificate.builder()
                .certType(request.getCertType())
                .issuedDate(request.getIssuedDate())
                .evidenceUrl(evidenceUrl)
                .user(user)
                .imagePublicId(imagePublicId)
                .build();

        certificate.updateScores(
                request.getListeningScore(),
                request.getReadingScore(),
                request.getSpeakingScore(),
                request.getWritingScore()
        );

        return certificateRepository.save(certificate);
    }

    @Transactional
    public Certificate updateCertificate(Certificate certificate, CertificateRequest request, String evidenceUrl, String imagePublicId) {
        certificate.setCertType(request.getCertType());
        certificate.setIssuedDate(request.getIssuedDate());
        if (evidenceUrl != null) {
            certificate.setEvidenceUrl(evidenceUrl);
            certificate.setImagePublicId(imagePublicId);
        }

        certificate.updateScores(
                request.getListeningScore(),
                request.getReadingScore(),
                request.getSpeakingScore(),
                request.getWritingScore()
        );

        return certificateRepository.save(certificate);
    }

    @Transactional
    public void deleteCertificate(Certificate certificate) {
        certificateRepository.delete(certificate);
    }
}
