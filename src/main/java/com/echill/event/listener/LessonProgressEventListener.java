package com.echill.event.listener;

import com.echill.entity.LessonProgress;
import com.echill.event.QuizPassedEvent;
import com.echill.repository.LessonProgressRepository;
import com.echill.repository.TestSetRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
public class LessonProgressEventListener {
    TestSetRepository testSetRepository;
    LessonProgressRepository progressRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleQuizPassed(QuizPassedEvent event) {

        try {
            Long lessonId = testSetRepository.findLessonIdByTestSetId(event.testSetId())
                    .orElse(null);

            if (lessonId == null) {
                log.debug("TestSet {} không gắn với Lesson nào, bỏ qua cập nhật Progress.", event.testSetId());
                return;
            }

            LessonProgress progress = progressRepository.findWithLessonByLessonIdAndEnrollmentStudentId(lessonId, event.studentId())
                    .orElse(null);

            if (progress == null) {
                log.warn("Không tìm thấy Progress cho Student {} tại Lesson {}", event.studentId(), lessonId);
                return;
            }

            progress.setIsQuizPassed(true);

            boolean isVideoAlreadyDone = Boolean.TRUE.equals(progress.getIsVideoWatched());

            if (isVideoAlreadyDone) {
                Integer currentVersion = progress.getLesson().getVersion();
                progress.markAsCompleted(currentVersion);
                log.info("🏆 [LESSON COMPLETED by QUIZ] User {} đã pass Test và hoàn thành bài {}.",
                        event.studentId(), lessonId);
            } else {
                log.info("⏳ [WAITING VIDEO] User {} đã pass Test nhưng chưa xem xong video bài {}.",
                        event.studentId(), lessonId);
            }

            progressRepository.save(progress);

        } catch (Exception e) {
            log.error("🔥 Lỗi cập nhật Tiến độ sau khi Pass Quiz (Student: {}, TestSet: {}). Chi tiết: {}",
                    event.studentId(), event.testSetId(), e.getMessage());
        }
    }
}
