package com.echill.service;

import com.echill.dto.request.ConsultationRequest;
import com.echill.dto.request.ConsultationSearchRequest;
import com.echill.dto.response.ConsultationResponse;
import com.echill.dto.response.PageResponse;
import com.echill.entity.Consultation;
import com.echill.entity.User;
import com.echill.entity.enums.ConsultationStatus;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.ConsultationRepository;
import com.echill.repository.UserRepository;
import com.echill.repository.specification.ConsultationSpecification;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultationService {
    ConsultationRepository consultationRepository;
    UserRepository userRepository;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public void createRequest(ConsultationRequest request) {
        try {
            Optional<Consultation> existingOpt = consultationRepository.findByEmail(request.getEmail());

            if (existingOpt.isPresent()) {
                Consultation existing = existingOpt.get();

                existing.setFullName(request.getFullName());
                existing.setPhoneNumber(request.getPhoneNumber());
                existing.setBirthYear(request.getBirthYear());
                existing.setTopic(request.getTopic());

                if (existing.getHandledBy() != null) {
                    existing.setStatus(ConsultationStatus.IN_PROGRESS);
                } else {
                    existing.setStatus(ConsultationStatus.PENDING);
                }

            } else {
                Consultation newEntity = Consultation.builder()
                        .fullName(request.getFullName())
                        .email(request.getEmail())
                        .phoneNumber(request.getPhoneNumber())
                        .birthYear(request.getBirthYear())
                        .topic(request.getTopic())
                        .status(ConsultationStatus.PENDING)
                        .build();

                consultationRepository.saveAndFlush(newEntity);
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition detected: Yêu cầu tư vấn với email {} vừa được tạo bởi một luồng khác.", request.getEmail());
        }
    }

    @Transactional
    public ConsultationResponse claimRequest(Long requestId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        int updatedRows = consultationRepository.assignConsultation(requestId, admin);

        if (updatedRows == 0) {
            throw new AppException(ErrorEnum.CONSULTATION_ALREADY_CLAIMED);
        }

        Consultation updatedConsultation = consultationRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorEnum.CONSULTATION_NOT_FOUND));

        return new ConsultationResponse(
                updatedConsultation.getId(),
                updatedConsultation.getFullName(),
                updatedConsultation.getEmail(),
                updatedConsultation.getPhoneNumber(),
                updatedConsultation.getBirthYear(),
                updatedConsultation.getTopic(),
                updatedConsultation.getStatus(),
                updatedConsultation.getCreatedAt(),
                admin.getId(),
                admin.getFullName()
        );
    }

    @Transactional
    public ConsultationResponse markAsCompleted(Long requestId) {
        Consultation request = consultationRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorEnum.CONSULTATION_NOT_FOUND));

        if (request.getHandledBy() == null || !request.getHandledBy().getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AppException(ErrorEnum.UNAUTHORIZED);
        }

        request.setStatus(ConsultationStatus.COMPLETED);

        return new ConsultationResponse(
                request.getId(),
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getBirthYear(),
                request.getTopic(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getHandledBy().getId(),
                request.getHandledBy().getFullName()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultationResponse> getConsultations(ConsultationSearchRequest request) {
        Specification<Consultation> spec = ConsultationSpecification.filter(request);

        Page<Consultation> pageData = consultationRepository.findAll(spec, request.getPageable());

        Page<ConsultationResponse> dtoPage = pageData.map(c -> new ConsultationResponse(
                c.getId(),
                c.getFullName(),
                c.getEmail(),
                c.getPhoneNumber(),
                c.getBirthYear(),
                c.getTopic(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getHandledBy() != null ? c.getHandledBy().getId() : null,
                c.getHandledBy() != null ? c.getHandledBy().getFullName() : null
        ));

        return PageResponse.of(dtoPage);
    }
}
