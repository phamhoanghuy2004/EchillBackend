package com.echill.service.persistence;

import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Lesson;
import com.echill.event.CourseUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.LessonMapper;
import com.echill.repository.LessonRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonPersistenceService {

    LessonRepository lessonRepository;
    LessonMapper lessonMapper;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public LessonResponse saveVideoDraft(Long lessonId, SaveVideoDraftRequest request) {
        Lesson lesson = lessonRepository.findByIdWithCourseAndTeacherAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        lesson.startVideoProcessing(request.getPublicVideoId(), request.getRawUrl());
        lessonRepository.save(lesson);

        log.info("✅ Đã chốt file nháp cho Lesson ID {}. Trạng thái: PROCESSING", lessonId);

        return lessonMapper.toLessonResponse(lesson);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Lesson processCloudinaryWebhook(Map<String, Object> payload) {
        String publicId = (String) payload.get("public_id");
        if (publicId == null) return null;

        Lesson lesson = lessonRepository.findByPublicVideoId(publicId).orElse(null);
        if (lesson == null) return null;

        if (!lesson.isVideoProcessing()) {
            log.info("Bỏ qua Webhook: Lesson ID {} đang ở trạng thái {}, không cần xử lý lại!", lesson.getId(), lesson.getVideoStatus());
            return lesson;
        }

        String hlsUrl = null;
        List<Map<String, Object>> eagerArray = (List<Map<String, Object>>) payload.get("eager");
        if (eagerArray != null && !eagerArray.isEmpty()) {
            Object secureUrlObj = eagerArray.getFirst().get("secure_url");
            if (secureUrlObj != null) {
                hlsUrl = secureUrlObj.toString();
            }
        }


        Long durationSeconds = 0L;
        try {
            double duration = 0.0;
            if (payload.get("duration") != null) {
                duration = Double.parseDouble(payload.get("duration").toString());
            } else if (eagerArray != null && !eagerArray.isEmpty() && eagerArray.getFirst().get("duration") != null) {
                duration = Double.parseDouble(eagerArray.getFirst().get("duration").toString());
            }
            if (duration > 0) {
                durationSeconds = (long) Math.round(duration);
            }
        } catch (NumberFormatException e) {
            log.warn("Lỗi ép kiểu duration cho publicId: {}", publicId);
        }

        lesson.finishVideoProcessing(hlsUrl, durationSeconds);

        Lesson savedLesson = lessonRepository.save(lesson);
        log.info("🚀 Hoàn tất Webhook, đã ra lệnh cho Lesson ID: {} chuyển sang READY", lesson.getId());

        eventPublisher.publishEvent(new CourseUpdatedEvent(savedLesson.getCourse().getId()));
        return savedLesson;
    }

}
