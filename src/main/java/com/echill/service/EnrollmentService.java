package com.echill.service;

import com.echill.dto.request.leaner.GetMyCoursesRequest;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.learner.CurriculumResponse;
import com.echill.dto.response.learner.LessonDetailResponse;
import com.echill.dto.response.learner.LessonItemResponse;
import com.echill.dto.response.learner.MyCourseResponse;
import com.echill.entity.*;
import com.echill.entity.enums.EnrollmentStatus;
import com.echill.entity.enums.LessonStatus;
import com.echill.entity.enums.VideoStatus;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.StudentErrorEnum;
import com.echill.mapper.LessonMapper;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.LessonProgressRepository;
import com.echill.repository.LessonRepository;
import com.echill.repository.TransactionRepository;
import com.echill.repository.projection.LessonWithProgressProjection;
import com.echill.repository.projection.MyCourseProjection;
import com.echill.util.SecurityUtils;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentService {
    EnrollmentRepository enrollmentRepository;
    TransactionRepository transactionRepository;
    LessonRepository lessonRepository;
    LessonProgressRepository lessonProgressRepository;
    LessonMapper lessonMapper;

    @Lazy
    @NonFinal
    @Autowired
    private EnrollmentService self;

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

    @Transactional
    public CurriculumResponse getCourseCurriculum(Long courseId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(currentUserId, courseId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.NOT_ENROLLED));

        if (enrollment.getEnrollmentStatus() != EnrollmentStatus.ACTIVE) {
            throw new AppException(StudentErrorEnum.COURSE_LOCKED);
        }

        enrollment.recordAccess();

        Course course = enrollment.getCourse();

        List<LessonWithProgressProjection> projections = lessonRepository
                .findLessonsWithProgress(courseId, enrollment.getId());

        List<LessonItemResponse> lessonItems = projections.stream().map(proj ->
                LessonItemResponse.builder()
                        .lessonId(proj.getLessonId())
                        .title(proj.getTitle())
                        .displayOrder(proj.getDisplayOrder())
                        .durationSeconds(proj.getDurationSeconds())
                        .hasVideo(VideoStatus.READY.equals(proj.getVideoStatus()))
                        .hasDocument(proj.getHasDocument())
                        .hasTest(proj.getHasTest())
                        .status(calculateLessonStatus(proj))
                        .lastWatchedSecond(proj.getLastWatchedSecond())
                        .build()
        ).toList();

        long completedCount = lessonItems.stream()
                .filter(item -> LessonStatus.COMPLETED.equals(item.getStatus()))
                .count();

        int totalRealLessons = lessonItems.size();

        int progressPercent = 0;
        if (totalRealLessons > 0) {
            progressPercent = (int) Math.round((completedCount * 100.0) / totalRealLessons);
            progressPercent = Math.min(progressPercent, 100);
        }

        return CurriculumResponse.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .totalLessons(totalRealLessons)
                .completedLessons(completedCount)
                .progressPercent(progressPercent)
                .lessons(lessonItems)
                .build();
    }

    private LessonStatus calculateLessonStatus(LessonWithProgressProjection proj) {
        if (proj.getProgressId() == null) {
            return LessonStatus.NOT_STARTED;
        }
        if (Boolean.TRUE.equals(proj.getIsCompleted())) {
            if (proj.getVersionCompleted() != null && proj.getVersionCompleted().equals(proj.getLessonVersion())) {
                return LessonStatus.COMPLETED;
            } else {
                return LessonStatus.OUTDATED;
            }
        }
        return LessonStatus.IN_PROGRESS;
    }

    @Transactional
    public void startLesson(Long lessonId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(currentUserId, lesson.getCourse().getId())
                .orElseThrow(() -> new AppException(StudentErrorEnum.NOT_ENROLLED));

        if (enrollment.getEnrollmentStatus() != EnrollmentStatus.ACTIVE) {
            throw new AppException(StudentErrorEnum.COURSE_LOCKED);
        }

        boolean hasUncompleted = lessonRepository.existsUncompletedPreviousLessons(
                lesson.getCourse().getId(),
                enrollment.getId(),
                lesson.getDisplayOrder()
        );

        if (hasUncompleted) {
            throw new AppException(StudentErrorEnum.PREVIOUS_LESSON_NOT_COMPLETED);
        }

        enrollment.recordAccess();

        try {
            LessonProgress newProgress = LessonProgress.builder()
                    .enrollment(enrollment)
                    .lesson(lesson)
                    .build();

            lessonProgressRepository.saveAndFlush(newProgress);

        } catch (DataIntegrityViolationException e) {
            log.warn("🔥 [Idempotency] User {} spam click start lesson {}. Đã chặn thành công!", currentUserId, lessonId);
        }
    }

    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonDetailForStudy(Long lessonId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        boolean hasStarted = lessonProgressRepository.existsByLessonIdAndEnrollmentStudentId(lessonId, currentUserId);

        if (!hasStarted) {
            log.warn("User {} cố gắng truy cập Lesson {} khi chưa Start!", currentUserId, lessonId);
            throw new AppException(StudentErrorEnum.LESSON_NOT_STARTED);
        }

        // 2. Pass bảo vệ rồi thì chọc vào Hàm Cache lấy Data dùng chung
        return self.getCachedLessonDetail(lessonId);
    }


    @Cacheable(value = "lessonDetails", key = "#lessonId")
    public LessonDetailResponse getCachedLessonDetail(Long lessonId) {
        log.info("🔥 CACHE MISS! Đang query Database cho Lesson ID: {}", lessonId);

        Lesson lesson = lessonRepository.findLessonWithDetailsById(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));
        return lessonMapper.toLessonDetailResponse(lesson);
    }
}
