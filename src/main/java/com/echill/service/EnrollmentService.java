package com.echill.service;

import com.echill.dto.request.leaner.GetMyCoursesRequest;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.learner.MyCourseResponse;
import com.echill.entity.*;
import com.echill.entity.enums.EnrollmentStatus;
import com.echill.event.TransactionSuccessEvent;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.TransactionRepository;
import com.echill.repository.projection.MyCourseProjection;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentService {
    EnrollmentRepository enrollmentRepository;
    TransactionRepository transactionRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTransactionSuccess(TransactionSuccessEvent event) {
        Long txId = event.transactionId();

        try {
            Transaction transaction = transactionRepository.findById(txId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy Transaction"));

            User student = transaction.getUser();
            if (student == null) {
                log.error("CRITICAL [Txn: {}] - Hóa đơn không có User (Student) gắn kèm! Hủy bỏ tiến trình cấp khóa học.", txId);
                return;
            }

            List<TransactionItem> items = transaction.getItems();
            if (items == null || items.isEmpty()) {
                log.warn("⚠️ BỎ QUA [Txn: {}] - Hóa đơn không có bất kỳ TransactionItem nào bên trong. Không có gì để cấp phát.", txId);
                return;
            }

            Set<Long> existingCourseIds = enrollmentRepository.findCourseIdsByStudentId(student.getId());
            List<Enrollment> newEnrollments = new ArrayList<>();

            for (TransactionItem item : items) {
                Course course = item.getCourse();
                if (course == null) continue;

                if (existingCourseIds.contains(course.getId())) {
                    log.info("User {} đã sở hữu khóa {}, bỏ qua.", student.getId(), course.getId());
                    continue;
                }

                Enrollment enrollment = Enrollment.builder()
                        .student(student)
                        .course(course)
                        .enrollmentStatus(EnrollmentStatus.ACTIVE)
                        .build();

                newEnrollments.add(enrollment);
            }

            if (!newEnrollments.isEmpty()) {
                try {
                    enrollmentRepository.saveAll(newEnrollments);
                    log.info("Đã cấp {} khóa học cho User {}", newEnrollments.size(), student.getId());
                } catch (DataIntegrityViolationException ex) {
                    log.warn("⚠️ Bỏ qua insert: Xung đột dữ liệu Unique Constraint (Có thể do Race Condition).", ex);
                }
            }

        } catch (Exception e) {
            log.error("CRITICAL: Lỗi cấp khóa học cho Transaction {}", txId, e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MyCourseResponse> getMyLearningCourses(GetMyCoursesRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Pageable safePageable = request.getPageable();

        Page<MyCourseProjection> projections = enrollmentRepository.findMyCoursesWithProgress(currentUserId, safePageable);

        Page<MyCourseResponse> responsePage = projections.map(this::mapToResponse);

        return PageResponse.of(responsePage);
    }

    private MyCourseResponse mapToResponse(MyCourseProjection proj) {
        int progressPercent = 0;

        if (proj.getTotalLessons() != null && proj.getTotalLessons() > 0) {
            long completed = proj.getCompletedLessons();

            progressPercent = (int) Math.round((completed * 100.0) / proj.getTotalLessons());
            progressPercent = Math.min(progressPercent, 100);
        }

        return MyCourseResponse.builder()
                .enrollmentId(proj.getEnrollmentId())
                .courseId(proj.getCourseId())
                .courseName(proj.getCourseName())
                .courseImage(proj.getCourseImage())
                .teacherName(proj.getTeacherName())
                .teacherAvatar(proj.getTeacherAvatar())
                .lastAccessedAt(proj.getLastAccessedAt())
                .totalLessons(proj.getTotalLessons())
                .completedLessons(proj.getCompletedLessons())
                .progressPercent(progressPercent)
                .build();
    }
}
